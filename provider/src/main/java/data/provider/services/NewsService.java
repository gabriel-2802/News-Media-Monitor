package data.provider.services;

import data.provider.dto.requests.NewsSourceRequest;
import data.provider.dto.responses.NewsSourceDto;
import data.provider.exceptions.BusinessException;
import data.provider.models.NewsSource;
import data.provider.repositories.ArticleRepository;
import data.provider.repositories.NewsSourceRepository;
import data.provider.util.Constants;
import data.provider.util.Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class NewsService {

    private final ArticleRepository articleRepository;
    private final NewsSourceRepository newsSourceRepository;

    public NewsSourceDto addNewsSource(final NewsSourceRequest newsSourceRequest) {
        if (newsSourceRepository.existsByBaseUrl(newsSourceRequest.baseUrl())) {
            throw new BusinessException(Constants.NEWS_SOURCE_BASE_URL_EXISTS_ERROR, newsSourceRequest.baseUrl());
        }

        if (newsSourceRepository.existsByName(newsSourceRequest.name())) {
            throw new BusinessException(Constants.NEWS_SOURCE_NAME_EXISTS_ERROR, newsSourceRequest.name());
        }

        if (newsSourceRepository.existsByRssUrl(newsSourceRequest.rssUrl())) {
            throw new BusinessException(Constants.NEWS_SOURCE_RSS_URL_EXISTS_ERROR, newsSourceRequest.rssUrl());
        }

        validateReachability(newsSourceRequest);

        final NewsSource source = NewsSource.builder()
                .baseUrl(newsSourceRequest.baseUrl())
                .name(newsSourceRequest.name())
                .rssUrl(newsSourceRequest.rssUrl())
                .notes(newsSourceRequest.notes())
                .politicalView(newsSourceRequest.politicalView())
                .sources(newsSourceRequest.sources())
                .biasScores(newsSourceRequest.biasScores())
                .build();

        newsSourceRepository.save(source);
        log.info(Constants.NEWS_SOURCE_ADD_SUCCESS_LOG, source.getName(), source.getId());

        return new NewsSourceDto(source, 0);
    }

    public List<NewsSourceDto> getAllNewsSources(final int page, final int count) {
        return newsSourceRepository.findAll(PageRequest.of(page, count)).getContent().stream()
                .map(source -> new NewsSourceDto(source, articleRepository.countArticlesBySource(source.getName())))
                .toList();
    }

    public NewsSourceDto getNewsSourceByName(final String sourceName) {
        final NewsSource source = newsSourceRepository.findByName(sourceName)
                .orElseThrow(() -> new BusinessException(Constants.SOURCE_DOES_NOT_EXIST_ERROR, sourceName));

        return new NewsSourceDto(source, articleRepository.countArticlesBySource(source.getName()));
    }

    public NewsSourceDto updateNewsSource(final String sourceName, final NewsSourceRequest newsSourceRequest) {
        final NewsSource existingSource = newsSourceRepository.findByName(sourceName)
                .orElseThrow(() -> new BusinessException(Constants.SOURCE_DOES_NOT_EXIST_ERROR, sourceName));

        if (!existingSource.getBaseUrl().equals(newsSourceRequest.baseUrl()) && newsSourceRepository.existsByBaseUrl(newsSourceRequest.baseUrl())) {
            throw new BusinessException(Constants.NEWS_SOURCE_BASE_URL_EXISTS_ERROR, newsSourceRequest.baseUrl());
        }

        if (!existingSource.getRssUrl().equals(newsSourceRequest.rssUrl()) && newsSourceRepository.existsByRssUrl(newsSourceRequest.rssUrl())) {
            throw new BusinessException(Constants.NEWS_SOURCE_RSS_URL_EXISTS_ERROR, newsSourceRequest.rssUrl());
        }

        validateReachability(newsSourceRequest);

        final NewsSource updatedSource = NewsSource.builder()
                .id(existingSource.getId())
                .name(newsSourceRequest.name())
                .baseUrl(newsSourceRequest.baseUrl())
                .rssUrl(newsSourceRequest.rssUrl())
                .notes(newsSourceRequest.notes())
                .politicalView(newsSourceRequest.politicalView())
                .sources(newsSourceRequest.sources())
                .biasScores(newsSourceRequest.biasScores())
                .failureCount(existingSource.getFailureCount())
                .isDisabled(existingSource.getIsDisabled())
                .build();

        newsSourceRepository.save(updatedSource);
        log.info(Constants.NEWS_SOURCE_UPDATE_SUCCESS_LOG, updatedSource.getName(), updatedSource.getId());

        return new NewsSourceDto(updatedSource, articleRepository.countArticlesBySource(updatedSource.getName()));
    }

    public NewsSourceDto incrementFailureCount(final String sourceName) {
        final NewsSource source = newsSourceRepository.incrementFailureCount(sourceName)
                .orElseThrow(() -> new BusinessException(Constants.SOURCE_DOES_NOT_EXIST_ERROR, sourceName));

        return new NewsSourceDto(source, articleRepository.countArticlesBySource(source.getName()));
    }

    public NewsSourceDto resetFailureCount(final String sourceName) {
        final NewsSource source = newsSourceRepository.resetFailureCount(sourceName)
                .orElseThrow(() -> new BusinessException(Constants.SOURCE_DOES_NOT_EXIST_ERROR, sourceName));

        return new NewsSourceDto(source, articleRepository.countArticlesBySource(source.getName()));
    }

    private void validateReachability(final NewsSourceRequest newsSourceRequest) {
        if (Util.isNotReachable(newsSourceRequest.rssUrl()) || Util.isNotReachable(newsSourceRequest.baseUrl())) {
            throw new BusinessException(Constants.NEWS_SOURCE_URLS_UNREACHABLE_ERROR,
                    newsSourceRequest.baseUrl(), newsSourceRequest.rssUrl());
        }
    }
}