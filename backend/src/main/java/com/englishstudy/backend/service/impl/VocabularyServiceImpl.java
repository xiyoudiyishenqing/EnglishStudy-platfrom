package com.englishstudy.backend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.englishstudy.backend.common.BusinessException;
import com.englishstudy.backend.constant.RoleConstants;
import com.englishstudy.backend.entity.StudentWordRecord;
import com.englishstudy.backend.entity.VocabularyWord;
import com.englishstudy.backend.mapper.StudentWordRecordMapper;
import com.englishstudy.backend.mapper.VocabularyWordMapper;
import com.englishstudy.backend.request.VocabularyRequests;
import com.englishstudy.backend.service.BaseService;
import com.englishstudy.backend.service.LogService;
import com.englishstudy.backend.service.VocabularyService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class VocabularyServiceImpl extends BaseService implements VocabularyService {

    private static final String MODE_REVIEW = "review";
    private static final String STATUS_NEW = "NEW";
    private static final String STATUS_MASTERED = "MASTERED";
    private static final String STATUS_REVIEW = "REVIEW";
    private static final String RESULT_KNOWN = "KNOWN";
    private static final String RESULT_UNKNOWN = "UNKNOWN";

    private final VocabularyWordMapper vocabularyWordMapper;
    private final StudentWordRecordMapper studentWordRecordMapper;
    private final LogService logService;

    public VocabularyServiceImpl(VocabularyWordMapper vocabularyWordMapper,
                                 StudentWordRecordMapper studentWordRecordMapper,
                                 LogService logService) {
        this.vocabularyWordMapper = vocabularyWordMapper;
        this.studentWordRecordMapper = studentWordRecordMapper;
        this.logService = logService;
    }

    @Override
    public Map<String, Object> listWords(String mode) {
        requireRole(RoleConstants.STUDENT);
        List<VocabularyWord> words = vocabularyWordMapper.selectList(Wrappers.<VocabularyWord>lambdaQuery()
                .orderByAsc(VocabularyWord::getSortOrder)
                .orderByAsc(VocabularyWord::getId));
        List<StudentWordRecord> records = listMineRecords();
        Map<Long, StudentWordRecord> recordMap = records.stream()
                .filter(record -> record.getWordId() != null)
                .collect(Collectors.toMap(StudentWordRecord::getWordId, Function.identity(), (first, second) -> first));
        boolean reviewMode = MODE_REVIEW.equalsIgnoreCase(trimToEmpty(mode));
        List<Map<String, Object>> views = words.stream()
                .map(word -> wordView(word, recordMap.get(word.getId())))
                .filter(view -> !reviewMode || STATUS_REVIEW.equals(view.get("status")))
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mode", reviewMode ? MODE_REVIEW : "all");
        result.put("stats", buildStats(words.size(), records));
        result.put("words", views);
        return result;
    }

    @Override
    public Map<String, Object> review(VocabularyRequests.WordReviewRequest request) {
        requireRole(RoleConstants.STUDENT);
        if (request == null || request.getWordId() == null) {
            throw new BusinessException("请选择要学习的单词");
        }
        VocabularyWord word = vocabularyWordMapper.selectById(request.getWordId());
        if (word == null) {
            throw new BusinessException("单词不存在");
        }

        StudentWordRecord record = findMineRecord(word.getId());
        boolean insert = false;
        if (record == null) {
            record = new StudentWordRecord();
            record.setStudentId(currentUser().getUserId());
            record.setWordId(word.getId());
            record.setStatus(STATUS_NEW);
            record.setReviewCount(0);
            record.setKnownCount(0);
            record.setUnknownCount(0);
            insert = true;
        }

        boolean known = Boolean.TRUE.equals(request.getKnown());
        record.setReviewCount(number(record.getReviewCount()) + 1);
        if (known) {
            record.setKnownCount(number(record.getKnownCount()) + 1);
            record.setStatus(STATUS_MASTERED);
            record.setLastResult(RESULT_KNOWN);
        } else {
            record.setUnknownCount(number(record.getUnknownCount()) + 1);
            record.setStatus(STATUS_REVIEW);
            record.setLastResult(RESULT_UNKNOWN);
        }
        record.setLastReviewedAt(LocalDateTime.now());

        if (insert) {
            markForInsert(record);
            studentWordRecordMapper.insert(record);
        } else {
            markForUpdate(record);
            studentWordRecordMapper.updateById(record);
        }

        logService.save("词汇学习", known ? "认识单词" : "加入错词", word.getWord());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("word", wordView(word, record));
        result.put("stats", buildStats(countWords(), listMineRecords()));
        return result;
    }

    @Override
    public void resetMine() {
        requireRole(RoleConstants.STUDENT);
        studentWordRecordMapper.delete(Wrappers.<StudentWordRecord>lambdaQuery()
                .eq(StudentWordRecord::getStudentId, currentUser().getUserId()));
        logService.save("词汇学习", "重置记录", "清空个人单词学习记录");
    }

    private List<StudentWordRecord> listMineRecords() {
        return studentWordRecordMapper.selectList(Wrappers.<StudentWordRecord>lambdaQuery()
                .eq(StudentWordRecord::getStudentId, currentUser().getUserId()));
    }

    private StudentWordRecord findMineRecord(Long wordId) {
        return studentWordRecordMapper.selectOne(Wrappers.<StudentWordRecord>lambdaQuery()
                .eq(StudentWordRecord::getStudentId, currentUser().getUserId())
                .eq(StudentWordRecord::getWordId, wordId)
                .last("limit 1"));
    }

    private Map<String, Object> wordView(VocabularyWord word, StudentWordRecord record) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", word.getId());
        view.put("word", word.getWord());
        view.put("phonetic", word.getPhonetic());
        view.put("meaning", word.getMeaning());
        view.put("exampleSentence", word.getExampleSentence());
        view.put("exampleTranslation", word.getExampleTranslation());
        view.put("difficulty", word.getDifficulty());
        view.put("sortOrder", word.getSortOrder());
        view.put("status", record == null ? STATUS_NEW : record.getStatus());
        view.put("reviewCount", record == null ? 0 : number(record.getReviewCount()));
        view.put("knownCount", record == null ? 0 : number(record.getKnownCount()));
        view.put("unknownCount", record == null ? 0 : number(record.getUnknownCount()));
        view.put("lastResult", record == null ? null : record.getLastResult());
        view.put("lastReviewedAt", record == null ? null : record.getLastReviewedAt());
        return view;
    }

    private Map<String, Object> buildStats(int totalCount, List<StudentWordRecord> records) {
        int studiedCount = (int) records.stream().filter(record -> number(record.getReviewCount()) > 0).count();
        int masteredCount = (int) records.stream().filter(record -> STATUS_MASTERED.equals(record.getStatus())).count();
        int reviewCount = (int) records.stream().filter(record -> STATUS_REVIEW.equals(record.getStatus())).count();
        int reviewTotal = records.stream().mapToInt(record -> number(record.getReviewCount())).sum();
        int knownTotal = records.stream().mapToInt(record -> number(record.getKnownCount())).sum();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalCount", totalCount);
        stats.put("studiedCount", studiedCount);
        stats.put("masteredCount", masteredCount);
        stats.put("reviewCount", reviewCount);
        stats.put("remainingCount", Math.max(totalCount - masteredCount, 0));
        stats.put("accuracyRate", reviewTotal == 0 ? 0 : Math.round(knownTotal * 100.0 / reviewTotal));
        return stats;
    }

    private int countWords() {
        Number count = vocabularyWordMapper.selectCount(Wrappers.<VocabularyWord>lambdaQuery());
        return count == null ? 0 : count.intValue();
    }

    private int number(Integer value) {
        return value == null ? 0 : value;
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
