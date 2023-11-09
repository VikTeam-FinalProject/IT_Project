import os
import time
from typing import Self
from selenium import webdriver
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from bs4 import BeautifulSoup 
import codecs
import re
from selenium.webdriver.common.by import By
import requests
from webdriver_manager.chrome import ChromeDriverManager
from django.core.files.base import ContentFile


class Crawler:
    __data = None

    def __init__(self):
        self.driver = self.__setupDriver()

    def getData(self):
        return self.__data
    
    def __setData(self, data):
        self.__data = data

    def __setupDriver(self):
        
        options = webdriver.ChromeOptions()
        options.add_argument('--headless')
        options.add_argument('--no-sandbox')
        options.add_argument('--disable-gpu')
        options.add_argument('--disable-web-security')
        options.add_argument('--allow-running-insecure-content')
        options.add_argument('--allow-cross-origin-auth-prompt')
        options.add_experimental_option("excludeSwitches", ["enable_logging"])

        driver = webdriver.Chrome(options=options)
        return driver
    
    def __scroll_down_page(self, times=5):
        # Scroll down the page to load more images (you can customize the number of scrolls)
        for _ in range(times):
            self.driver.execute_script("window.scrollTo(0, document.body.scrollHeight);")
            time.sleep(2)

    def __crawl(self, query):
        images_dict = {} 
        try:
            search_url = f'https://www.google.com/search?q={query}&sca_esv=578460548&tbm=isch&sxsrf=AM9HkKk_WcaCaBLFuAKyKfvDk8fZwHMEQw:1698836765136&source=lnms&sa=X&ved=2ahUKEwiP9aqP1KKCAxUhklYBHfiPC-IQ_AUoAXoECAEQAw&biw=1492&bih=704&dpr=1.25'
            
            self.driver.get(search_url)
            time.sleep(5)

            self.__scroll_down_page()

            soup = BeautifulSoup(self.driver.page_source, 'html.parser')

            image_container = soup.find(id='islmp')

            for idx, raw_img in enumerate(image_container.find_all('img'), 1):
                link = raw_img.get('data-src')
                if link and link.startswith("https://") and link.__contains__('images'):
                    response = requests.get(link)
                    if response.status_code == 200:
                        img_name = f"image_{idx}.jpg"
                        # images_dict[link] = ContentFile(response.content, img_name)
                        images_dict[img_name] = link


        except Exception as e:
            print("An error occurred:", str(e))

        finally:
            self.driver.quit()

        
        return images_dict
    
    def crawl(self, query):
        data = self.__crawl(query)
        self.__setData(data)

    





if __name__ == "__main__":
    crawler = Crawler()
    crawler.crawl("apple+banana+orange")
    foo = crawler.getData()

