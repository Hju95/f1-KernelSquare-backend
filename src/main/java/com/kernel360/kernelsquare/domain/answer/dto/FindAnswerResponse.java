package com.kernel360.kernelsquare.domain.answer.dto;

import com.kernel360.kernelsquare.domain.answer.entity.Answer;
import com.kernel360.kernelsquare.domain.image.utils.ImageUtils;

import java.time.LocalDateTime;

public record FindAnswerResponse(
        Long answerId,
        Long questionId,
        String content,
        String rankImageUrl,
        String memberImageUrl,
        String createdBy,
        Long authorLevel,
        String answerImageUrl,
        LocalDateTime createdDate,
        LocalDateTime modifiedDate,
        Long voteCount,
        Long voteStatus
) {
    public static FindAnswerResponse from(Answer answer, String rankImageUrl, Long authorLevel, Long voteStatus) {
        return new FindAnswerResponse(
                answer.getId(),
                answer.getQuestion().getId(),
                answer.getContent(),
                rankImageUrl,
                answer.getMember().getImageUrl(),
                answer.getMember().getNickname(),
                authorLevel,
                ImageUtils.makeImageUrl(answer.getImageUrl()),
                answer.getCreatedDate(),
                answer.getModifiedDate(),
                answer.getVoteCount(),
                voteStatus
        );
    }
}
