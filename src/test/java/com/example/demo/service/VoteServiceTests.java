package com.example.demo.service;

import com.example.demo.auth.dto.response.MessageDto;
import com.example.demo.dto.request.CommentRequestDto;
import com.example.demo.dto.request.VoteRequestDto;
import com.example.demo.dto.response.VoteResponseDto;
import com.example.demo.entity.*;
import com.example.demo.enumeration.EVoteType;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.PostRepository;
import com.example.demo.repository.VoteRepository;
import com.example.demo.service.impl.VoteServiceImpl;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VoteServiceTests {
    @Mock
    private VoteRepository voteRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private IUtilService utilService;
    @Spy
    private ModelMapper modelMapper;
    @InjectMocks
    private VoteServiceImpl voteService;

    private User user;
    private Vote vote;

    @BeforeEach
    void setup() {
        user = User.builder()
                .id(1L)
                .username("author")
                .email("test@test.com")
                .password("test")
                .build();

        Post post = Post.builder().id(1L).build();

        vote = Vote.builder()
                .id(1L)
                .user(user)
                .type(EVoteType.UPVOTE)
                .post(post)
                .build();
    }

    @Test
    void testVotePost_Success_WhenVoteDoesNotExist_ReturnsNewVote() {
        given(utilService.getCurrentUser()).willReturn(user);
        given(voteRepository.findByUserIdAndPostId(anyLong(), anyLong())).willReturn(null);

        VoteRequestDto voteRequestDto = VoteRequestDto.builder()
                .type("UPVOTE")
                .build();

        given(postRepository.findById(anyLong())).willReturn(Optional.of(new Post()));

        Object result = voteService.votePost(1L, voteRequestDto);

        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(VoteResponseDto.class);
        verify(voteRepository, times(1)).save(any(Vote.class));
    }

    @Test
    void testVotePost_Success_WhenVoteExistsAndTypeIsEqual_ReturnsMessageDto() {
        given(utilService.getCurrentUser()).willReturn(user);
        given(voteRepository.findByUserIdAndPostId(anyLong(), anyLong())).willReturn(vote);

        VoteRequestDto voteRequestDto = VoteRequestDto.builder()
                .type("UPVOTE")
                .build();

        Object result = voteService.votePost(1L, voteRequestDto);

        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(MessageDto.class);
        verify(voteRepository, times(1)).deleteByUserIdAndPostId(anyLong(), anyLong());
        verify(voteRepository, never()).save(any(Vote.class));
    }

    @Test
    void testVotePost_Success_WhenVoteExistsAndTypeIsNotEqual_ReturnsUpdatedVote() {
        given(utilService.getCurrentUser()).willReturn(user);
        given(voteRepository.findByUserIdAndPostId(anyLong(), anyLong())).willReturn(vote);

        VoteRequestDto voteRequestDto = VoteRequestDto.builder()
                .type("DOWNVOTE")
                .build();

        VoteResponseDto result = (VoteResponseDto) voteService.votePost(1L, voteRequestDto);

        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(VoteResponseDto.class);
        assertEquals(voteRequestDto.getType(), result.getType());
        verify(voteRepository, never()).save(any(Vote.class));
    }

    @Test
    void testVotePost_Success_WhenPostNotFound_ThrowsResourceNotFoundException() {
        given(utilService.getCurrentUser()).willReturn(user);
        given(voteRepository.findByUserIdAndPostId(anyLong(), anyLong())).willReturn(null);

        VoteRequestDto voteRequestDto = VoteRequestDto.builder()
                .type("DOWNVOTE")
                .build();

        given(postRepository.findById(anyLong())).willReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                voteService.votePost(1L, voteRequestDto));

        verify(postRepository, times(1)).findById(anyLong());
        verify(voteRepository, never()).save(any(Vote.class));
    }
}
