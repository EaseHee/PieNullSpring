package com.acorn.process;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.acorn.dto.CommentsDto;
import com.acorn.entity.Comments;
import com.acorn.repository.CommentsRepository;
import com.acorn.repository.EateriesRepository;
import com.acorn.repository.MembersRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommentsProcess {
	private final CommentsRepository commentRepository;
	private final MembersRepository memberRepository;
	private final EateriesRepository eateryRepository;
	
	// Create
	@Transactional
	public CommentsDto createComment(CommentsDto dto) {
        Comments comment = dto.toEntity(memberRepository, eateryRepository);

        if (dto.getParentCommentNo() != null) {
            Comments parentComment = commentRepository.findById(dto.getParentCommentNo())
            	.orElseThrow(() -> new EntityNotFoundException("부모 댓글을 찾을 수 없습니다."));
            parentComment.addChildComment(comment);
        }
        return CommentsDto.fromEntity(commentRepository.save(comment));
    }
	
	// Read
	@Transactional(readOnly = true)
	public List<CommentsDto> getCommentsByEatery(int eateryNo) {
		return commentRepository.findByEateryNoOrderByCreatedAtDesc(eateryNo)
			.stream().map(CommentsDto::fromEntity).collect(Collectors.toList());
	}
	
	@Transactional(readOnly = true)
	public List<CommentsDto> getCommentsByMember(int memberNo) {
		return commentRepository.findByMemberNoOrderByCreatedAtDesc(memberNo)
			.stream().map(CommentsDto::fromEntity).collect(Collectors.toList());
	}
	
	// Update: 이건 @Transactional 없이 정상작동
	public CommentsDto updateComment(int no, CommentsDto updatedDto) {
	    Comments comment = commentRepository.findById(no)
	    	.orElseThrow(() -> new EntityNotFoundException("댓글을 찾을 수 없습니다."));
	    
	    return CommentsDto.fromEntity(commentRepository.save(comment.toBuilder().content(updatedDto.getContent()).build()));
	}

	// Delete
	@Transactional
	public void deleteComment(int no) {
	    Comments comment = commentRepository.findById(no)
	        .orElseThrow(() -> new EntityNotFoundException("댓글을 찾을 수 없습니다."));

	    // 자식이 있는 부모 댓글을 삭제하면 부모 댓글을 삭제상태로 변경
	    if (comment.hasChildComments()) {
	        Comments deletedComment = comment.toBuilder()
	            .isDeleted(true)
	            .content("이 댓글은 삭제되었습니다.")
	            .build();
	        commentRepository.save(deletedComment);
	    } else {
	    	// 자식을 삭제할 때 부모댓글의 자식 목록에서 해당 댓글을 제거
	    	Comments parentComment = comment.getParentComment();
	        if (parentComment != null) {
	        	parentComment.getChildComments().remove(comment);
	        	
	        	// 부모 댓글이 삭제상태이고 자식이 하나도 없을 경우 부모 댓글을 삭제
	        	if (parentComment.isDeleted() && !parentComment.hasChildComments()) {
	        		commentRepository.delete(parentComment);
	        	}
	        }
	        commentRepository.delete(comment); // 자식이 없는 댓글은 바로 삭제
	    }
	}
}
