from scrapy import signals
from scrapy.downloadermiddlewares.retry import RetryMiddleware
from scrapy.utils.python import global_object_name
from scrapy.utils.httpobj import urlparse_cached
import time



class WebcrawlerSpiderMiddleware(RetryMiddleware):

    def __init__(self,setting):
        super(WebcrawlerSpiderMiddleware, self).__init__(setting)
        self.cached_resolutions = {}
        # dns cache timeout set to 600s 
        self.cache_timeout = setting.getfloat('DNSCACHE_TIMEOUT', 600)
        # backoff factor applied to the retry delay
        self.retry_backoff = 2.0

    @classmethod
    def from_crawler(cls, crawler):
        s = super(WebcrawlerSpiderMiddleware, cls).from_crawler(crawler)
        crawler.signals.connect(s.spider_opened, signal=signals.spider_opened)
        return s
    
    def spider_opened(self, spider):
        spider.logger.info("Spider opened: %s" % spider.name)
        self.cached_resolutions = {}

    def _get_host(self, request):
        return urlparse_cached(request).hostname

    def _retry_dns(self, request, reason, spider):
        host = self._get_host(request)
        resolution = self.cached_resolutions.get(host)
        if resolution is not None and time.time() - resolution['timestamp'] < self.cache_timeout:
            # Return the cached IP address
            return resolution['ip']
        return super(WebcrawlerSpiderMiddleware, self)._retry_dns(request, reason, spider)
    
    def _retry(self, request, reason, spider):
        host = self._get_host(request)
        resolution = self.cached_resolutions.get(host)
        if resolution is not None and time.time() - resolution['timestamp'] < self.cache_timeout:
            # return cached IP address
            request.meta['proxy'] = resolution['ip']
            return super(WebcrawlerSpiderMiddleware, self)._retry(request, reason, spider)

        retry = super(WebcrawlerSpiderMiddleware, self)._retry(request, reason, spider)

        if retry is None:
            # store the IP address in the cache for future use
            ip = self._retry_dns(request, reason, spider)
            self.cached_resolutions[host] = {
                'ip': ip,
                'timestamp': time.time()
            }

        return retry




