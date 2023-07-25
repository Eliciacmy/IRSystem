BOT_NAME = "webcrawler"
SPIDER_MODULES = ["webcrawler.spiders"]
NEWSPIDER_MODULE = "webcrawler.spiders"
DUPEFILTER_CLASS = 'scrapy.dupefilters.RFPDupeFilter'
ROBOTSTXT_OBEY = True
DOWNLOADER_MIDDLEWARES = {
    'webcrawler.middlewares.WebcrawlerSpiderMiddleware': 543,
}
REQUEST_FINGERPRINTER_IMPLEMENTATION = "2.7"
FEED_EXPORT_ENCODING = "utf-8"

# prevent the crawler from timing out during long-running requests or operations, use retry 
RETRY_ENABLED = True
RETRY_TIMES = 5  # Number of times to retry a request before giving up
RETRY_HTTP_CODES = [500, 502, 503, 504, 522, 524, 408, 429]
DOWNLOAD_TIMEOUT = 600

