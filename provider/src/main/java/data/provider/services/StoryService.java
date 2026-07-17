package data.provider.services;

import data.provider.dto.requests.AttachArticleRequest;
import data.provider.dto.responses.ArticleDto;
import data.provider.dto.responses.StoryDto;
import data.provider.exceptions.BusinessException;
import data.provider.models.Story;
import data.provider.repositories.ArticleRepository;
import data.provider.repositories.StoryRepository;
import data.provider.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class StoryService {

    /**
     * Cap on articles fetched per story when hydrating full StoryDtos (see
     * getStories). Not the same as the story-list page size — this bounds
     * how many articles come back embedded in *each* story.
     */
    private static final int MAX_ARTICLES_PER_STORY = 100;

    private final StoryRepository storyRepository;
    private final ArticleRepository articleRepository;

    public StoryDto createStory(final String title) {
        final Instant now = Instant.now();
        final Story story = Story.builder()
                        .title(title)
                        .createdAt(now)
                        .lastUpdated(now)
                        .build();

        return new StoryDto(storyRepository.save(story));
    }

    public StoryDto attachArticle(final String storyId, final AttachArticleRequest request) {
        if (!storyRepository.existsById(storyId)) {
            throw new BusinessException(Constants.STORY_DOES_NOT_EXIST_ERROR, storyId);
        }
        if (!articleRepository.existsByUrl(request.articleUrl())) {
            throw new BusinessException(Constants.ARTICLE_DOES_NOT_EXIST_ERROR, request.articleUrl());
        }

        final Story updated = storyRepository.attachArticle(storyId, request.articleUrl(), Instant.now())
                .orElseThrow(() -> new BusinessException(Constants.STORY_DOES_NOT_EXIST_ERROR, storyId));

        return new StoryDto(updated);
    }

    public List<StoryDto> getRecentStories(final int days) {
        final Instant since = Instant.now().minus(days, ChronoUnit.DAYS);
        return storyRepository.findActiveSince(since).stream().map(StoryDto::new).toList();
    }

    public List<StoryDto> getStories(final int page, final int count) {
        return storyRepository.findAllPaginated(PageRequest.of(page, count))
                .getContent().stream()
                .map(story -> new StoryDto(story, getArticlesForStory(story.getId())))
                .toList();
    }

    private List<ArticleDto> getArticlesForStory(final String storyId) {
        return articleRepository.findByStoryId(storyId, PageRequest.of(0, MAX_ARTICLES_PER_STORY))
                .getContent().stream().map(ArticleDto::new).toList();
    }
}
