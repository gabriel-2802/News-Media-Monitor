package data.provider.services;

import data.provider.dto.requests.ArticleRequest;
import data.provider.dto.responses.ArticleDto;
import data.provider.exceptions.BusinessException;
import data.provider.models.Article;
import data.provider.models.NewsSource;
import data.provider.repositories.ArticleRepository;
import data.provider.repositories.NewsSourceRepository;
import data.provider.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final NewsSourceRepository newsSourceRepository;

    public List<ArticleDto> getAllArticles(final int page, final int count) {
        return articleRepository
                .findAllWithSource(PageRequest.of(page, count)).getContent().stream().map(ArticleDto::new).toList();
    }

    public List<ArticleDto> getAllArticlesFromSource(final int page, final int count, final String sourceName) {
        return articleRepository
                .findBySourceName(sourceName, PageRequest.of(page, count)).getContent().stream().map(ArticleDto::new).toList();
    }

    public ArticleDto saveArticle(final ArticleRequest articleRequest) {
        if (articleRepository.existsByUrl(articleRequest.url())) {
            throw new BusinessException(Constants.ARTICLE_EXISTS_ERROR, articleRequest.url());
        }

        final NewsSource source = newsSourceRepository.findByName(articleRequest.sourceName())
                .orElseThrow(() -> new BusinessException(Constants.SOURCE_DOES_NOT_EXIST_ERROR, articleRequest.sourceName()));

        if (!articleRequest.url().contains(source.getBaseUrl())) {
            throw new BusinessException(Constants.NEWS_SOURCE_ARTICLE_URL_MISMATCH_ERROR, articleRequest.url(), articleRequest.sourceName());
        }

        final Article article = Article.builder()
                .author(articleRequest.author())
                .title(articleRequest.title())
                .url(articleRequest.url())
                .bodyText(articleRequest.bodyText())
                .publishedAt(articleRequest.publishedAt())
                .newsSource(source)
                .build();

        articleRepository.save(article);
        return new ArticleDto(article);
    }

    public boolean existsByURL(final String url) {
        return articleRepository.existsByUrl(url);
    }
}
