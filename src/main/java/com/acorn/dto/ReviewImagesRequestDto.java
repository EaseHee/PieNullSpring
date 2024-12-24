package com.acorn.dto;

import com.acorn.entity.ReviewImages;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewImagesRequestDto {
    private String imageUrl;
    private ReviewsDto reviewsDto;
	// toDto
	public static ReviewImagesResponseDto toDto(ReviewImages reviewImages) {
		return ReviewImagesResponseDto.builder()
				.imageUrl(reviewImages.getImageUrl())
				.reviewsDto(ReviewsDto.toDto(reviewImages.getReviews()))
				.build();
	}
}
