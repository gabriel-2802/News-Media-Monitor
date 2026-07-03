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

        if (!newsSourceRequest.rssUrl().contains(Util.extractDomain(newsSourceRequest.baseUrl()))) {
            throw new BusinessException(Constants.NEWS_SOURCE_DOMAIN_MISMATCH_ERROR,
                    newsSourceRequest.baseUrl(), newsSourceRequest.rssUrl());
        }

        if (Util.isNotReachable(newsSourceRequest.rssUrl()) || Util.isNotReachable(newsSourceRequest.baseUrl())) {
            throw new BusinessException(Constants.NEWS_SOURCE_URLS_UNREACHABLE_ERROR,
                    newsSourceRequest.baseUrl(), newsSourceRequest.rssUrl());
        }

        final NewsSource source = NewsSource.builder()
                .baseUrl(newsSourceRequest.baseUrl())
                .name(newsSourceRequest.name())
                .rssUrl(newsSourceRequest.rssUrl())
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

    public NewsSourceDto incrementFailureCount(final String sourceName) {
        final NewsSource source = newsSourceRepository.incrementFailureCount(sourceName)
                .orElseThrow(() -> new BusinessException(Constants.SOURCE_DOES_NOT_EXIST_ERROR, sourceName));

        return new NewsSourceDto(source, articleRepository.countArticlesBySource(source.getName()));
    }
}