import scrapy
import hashlib
import re

class EncyclopediaSpider(scrapy.Spider):
    name = 'encyclopedia'
    allowed_domains = ['encyclopedia.com']
    custom_settings = {
        'DUPEFILTER_CLASS': 'scrapy.dupefilters.RFPDupeFilter',
        'DUPEFILTER_DEBUG': True,
        'DUPEFILTER_PERSIST': True,
    }
    start_urls = ['https://www.encyclopedia.com/sitemap.xml']
    def start_requests(self):
        urls = ['https://www.encyclopedia.com/sitemap.xml']
        for url in urls:
            yield scrapy.Request(url=url, callback=self.parse)

    def parse(self, response):
            loc_tags = response.xpath("//*[local-name()='loc'][not(contains(string(), 'sitemap_index'))]//text()").getall()
            for loc in loc_tags:
                yield scrapy.Request(url=loc, callback=self.save)

    def save(self, response):
            innerloc_tags = response.xpath("//*[local-name()='loc']//text()").getall()
            for loc in innerloc_tags:
                yield scrapy.Request(url=loc, callback=self.parsecontent)
    def parsecontent(self, response):
        #19 keywords only cause got 1 keyword "info" is repeated 
        keywords =['computer', 'glasgow', 'united', 'kingdom', 'library', 'fog', 'empires', 'doctor', 'hospital', 'bachelor',
'degree', 'internet', 'things', 'information', 'info', 'retrieval', 'retrieve', 'universe', 'university']
        matched_keywords = []
        hash_object = hashlib.sha256()
        for key in keywords:
            pattern = r'\b{}\b'.format(re.escape(key))
            if re.search(pattern, "".join(response.xpath("//*[@id='collapseExample0']//text()").getall()).lower(), flags=re.IGNORECASE) or re.search(pattern, "".join(response.xpath("//*[@class='doctitle']/text()").getall()), flags=re.IGNORECASE):
                matched_keywords.append(key)
        if matched_keywords != []:
            content="".join(response.xpath("//*[@id='collapseExample0']//text()").getall())
            hash_object.update(content.encode("utf-8"))
            hash_value = hash_object.hexdigest()
            yield{
                "docurl" : response.url,
                "tag": matched_keywords,
                "hash_content": hash_value,
                "title": " ".join(response.xpath("//*[@class='doctitle']//text()").getall()),
                "content": " ".join(response.xpath("//*[@id='collapseExample0']//text()").getall())
            }