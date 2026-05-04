-- qna 테이블에 llm_score 컬럼 추가
-- 기존 행은 reward_points 전체를 llm_score로 보정하지 않고 0으로 초기화 (수동 포인트 분리 불가)
ALTER TABLE qna ADD COLUMN llm_score INT NOT NULL DEFAULT 0;
