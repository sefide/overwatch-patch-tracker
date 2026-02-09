#!/usr/bin/env python3
"""
오버워치 패치 노트 스크래핑 테스트
"""

import requests
from bs4 import BeautifulSoup
from datetime import datetime
import re

def scrape_overwatch_patches():
    url = "https://overwatch.blizzard.com/en-us/news/patch-notes/live"
    
    print(f"🔍 패치 노트 페이지 접근 중: {url}")
    
    headers = {
        'User-Agent': 'Mozilla/5.0 (compatible; OverwatchPatchTracker/1.0)'
    }
    
    try:
        response = requests.get(url, headers=headers, timeout=10)
        response.raise_for_status()
        
        print(f"✅ 페이지 접근 성공 (상태 코드: {response.status_code})")
        
        soup = BeautifulSoup(response.text, 'html.parser')
        
        # 날짜 헤더 찾기 (h3 태그)
        date_headers = soup.find_all('h3')
        
        print(f"\n📅 발견된 헤더 수: {len(date_headers)}")
        
        patches = []
        
        for header in date_headers[:5]:  # 최근 5개만
            header_text = header.get_text().strip()
            
            # 날짜 패턴 매칭 (예: "January 20, 2026")
            date_match = re.search(r'([A-Z][a-z]+)\s+(\d{1,2}),\s+(\d{4})', header_text)
            
            if date_match:
                patch_date = date_match.group(0)
                print(f"\n{'='*60}")
                print(f"📌 패치 날짜: {patch_date}")
                
                # 영웅 헤더 찾기 (h5 태그)
                next_element = header.find_next_sibling()
                heroes = []
                
                while next_element and next_element.name != 'h3':
                    if next_element.name == 'h5':
                        hero_name = next_element.get_text().strip()
                        heroes.append(hero_name)
                        
                        # 변경사항 찾기
                        changes = []
                        change_element = next_element.find_next_sibling()
                        
                        while change_element and change_element.name not in ['h5', 'h3']:
                            if change_element.name == 'ul':
                                for li in change_element.find_all('li'):
                                    change_text = li.get_text().strip()
                                    if change_text:
                                        changes.append(change_text)
                            change_element = change_element.find_next_sibling()
                        
                        if changes:
                            print(f"\n  🦸 영웅: {hero_name}")
                            print(f"     변경사항 {len(changes)}개:")
                            for i, change in enumerate(changes[:3], 1):
                                # 버프/너프 판단
                                change_type = "📈 BUFF" if "increased" in change.lower() else \
                                             "📉 NERF" if "reduced" in change.lower() or "decreased" in change.lower() else \
                                             "🔧 ADJUSTMENT"
                                
                                # 수치 추출
                                value_match = re.search(r'from\s+(\d+\.?\d*)\s+to\s+(\d+\.?\d*)', change)
                                if value_match:
                                    prev_val = value_match.group(1)
                                    new_val = value_match.group(2)
                                    print(f"       {i}. {change_type} {change[:80]}...")
                                    print(f"          ({prev_val} → {new_val})")
                                else:
                                    print(f"       {i}. {change_type} {change[:80]}...")
                    
                    next_element = next_element.find_next_sibling()
                
                patches.append({
                    'date': patch_date,
                    'heroes': heroes
                })
        
        print(f"\n{'='*60}")
        print(f"\n✅ 총 {len(patches)}개 패치 발견")
        
        # 통계
        all_heroes = []
        for patch in patches:
            all_heroes.extend(patch['heroes'])
        
        from collections import Counter
        hero_counts = Counter(all_heroes)
        
        print(f"\n📊 영웅별 업데이트 빈도 (Top 10):")
        for hero, count in hero_counts.most_common(10):
            print(f"   {hero}: {count}회")
        
        return patches
        
    except requests.RequestException as e:
        print(f"❌ 에러 발생: {e}")
        return []

if __name__ == "__main__":
    print("=" * 60)
    print("🎮 Overwatch Patch Notes Scraper Test")
    print("=" * 60)
    scrape_overwatch_patches()
