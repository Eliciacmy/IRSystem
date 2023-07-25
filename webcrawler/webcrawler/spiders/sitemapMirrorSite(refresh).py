import scrapy
import hashlib
import re
import json
class EncyclopediaSpider(scrapy.Spider):
    name = 'encyclopedia_update'
    allowed_domains = ['encyclopedia.com']
    start_urls = ['https://www.encyclopedia.com/sitemap.xml']
    custom_settings = {
        'DUPEFILTER_CLASS': 'scrapy.dupefilters.RFPDupeFilter',
        'DUPEFILTER_DEBUG': True,
        'DUPEFILTER_PERSIST': True,
    }
    def __init__(self, *args, **kwargs):
        super(EncyclopediaSpider, self).__init__(*args, **kwargs)
        self.crawled_urls = self.load_existing_urls()

    def load_existing_urls(self):
        # Read content from the JSON file and extract the URLs
        try:
            with open('dataset.json', 'rb') as file:
                return json.load(file)
        except (FileNotFoundError, json.JSONDecodeError):
            return []
            
    def save_crawled_urls(self):
        with open('dataset.json', 'w') as file:
            json.dump(self.crawled_urls, file)      

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
                is_duplicate = False
                for data in self.crawled_urls:
                    if data['docurl'] == loc:
                        yield scrapy.Request(url=loc, callback=self.parsecontent_update, meta = {'hashvalue': data} )
                        is_duplicate = True
                        break
                if not is_duplicate:
                    yield scrapy.Request(url=loc, callback=self.parsecontent)
                
    def parsecontent_update(self, response):
        data = response.meta.get('hashvalue')
        keywords =['computer', 'glasgow', 'united', 'kingdom', 'library', 'fog', 'empires', 'doctor', 'hospital', 'bachelor','degree', 'internet', 'things', 'information', 'info', 'retrieval', 'retrieve', 'universe', 'university']
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
            if (data['hash_content'] != hash_object):
                data["docurl"]= response.url
                data["tag"]= matched_keywords
                data["hash_content"]= hash_value
                data["title"]= " ".join(response.xpath("//*[@class='doctitle']//text()").getall()),
                data["content"]= " ".join(response.xpath("//*[@id='collapseExample0']//text()").getall())
          
    def parsecontent(self, response):
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
            item = {
                "docurl" : response.url,
                "tag": matched_keywords,
                "hash_content": hash_value,
                "title": " ".join(response.xpath("//*[@class='doctitle']//text()").getall()),
                "content": " ".join(response.xpath("//*[@id='collapseExample0']//text()").getall())
            }
            self.crawled_urls.append(item)
    def closed(self, reason):
        self.save_crawled_urls()