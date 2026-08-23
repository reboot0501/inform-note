-- Docker 컨테이너에서 sqlplus 접속
docker exec -it oracle26ai-free sqlplus sys/leetj1485+@FREEPDB1 as sysdba

-- 사용자 생성
CREATE USER inform_note IDENTIFIED BY "leetj1485+";
GRANT CONNECT, RESOURCE, DBA TO inform_note;

-- docker container 이미지 내부의 inform_note user 로 로그인
docker exec -it oracle26ai-free sqlplus inform_note/leetj1485+@FREEPDB1

-- =====================================================================
-- 1. 다운 이벤트 로그 메인 테이블 (DOWN_EVENT_LOG)
-- =====================================================================
CREATE TABLE IF NOT EXISTS tb_down_event_log (
    down_event_id           VARCHAR2(50)            NOT NULL,
    equipment_id            VARCHAR2(50)            NOT NULL,
    chamber_id              VARCHAR2(50),
    fabrication_plant       VARCHAR2(30)            NOT NULL, -- Enum: FabricationPlant
    process_module          VARCHAR2(30)            NOT NULL, -- Enum: ProcessModule
    equipment_model         VARCHAR2(50)            NOT NULL, -- Enum: EquipmentModel
    down_type               VARCHAR2(30)            NOT NULL, -- Enum: DownType
    work_status             VARCHAR2(30)            NOT NULL, -- Enum: WorkStatus
    down_start_datetime     NUMBER(19)              NOT NULL, -- long (Epoch ms)
    down_end_datetime       NUMBER(19),                       -- long (Epoch ms)
    down_duration_minutes   NUMBER(10),                       -- long
    is_critical             BOOLEAN DEFAULT FALSE   NOT NULL,
    down_code               VARCHAR2(50)            NOT NULL,
    down_code_description   VARCHAR2(500),
    alarm_id                VARCHAR2(50),
    assigned_technician     JSON,                             -- VO 객체: AssignedTechnician
    approver                JSON,                             -- VO 객체: Approver
    part_replacements       JSON,                             -- List<PartReplacement> 배열
	created_by          	VARCHAR2(50),
    created_at              TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
	updated_by          	VARCHAR2(50),
    updated_at              TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL
);

-- 2. 기본키(PK) 제약조건 추가
ALTER TABLE tb_down_event_log
    ADD CONSTRAINT pk_down_event_log PRIMARY KEY (down_event_id);

-- =====================================================================
-- 2. 관계형 및 Enum 복합 인덱스 (RDB 핵심 조회 최적화)
-- =====================================================================

-- 공장/모듈/상태 및 기간 검색 복합 인덱스
CREATE INDEX idx_down_event_search 
    ON tb_down_event_log (fabrication_plant, process_module, work_status, down_start_datetime DESC);

-- 장비/챔버별 기간 이력 조회 인덱스
CREATE INDEX idx_down_event_eqp_hist 
    ON tb_down_event_log (equipment_id, chamber_id, down_start_datetime DESC);

-- 다운 코드/타입별 통계 인덱스
CREATE INDEX idx_down_event_code 
    ON tb_down_event_log (down_code, down_type);


-- =====================================================================
-- 3. JSON VO 객체 및 배열 인덱스 (Oracle 23ai/26ai 특화)
-- =====================================================================

-- [VO 객체 인덱스] 담당 엔지니어 사번(technicianId) B-Tree 인덱스
CREATE INDEX idx_json_tech_id 
    ON tb_down_event_log (JSON_VALUE(assigned_technician, '$.technicianId' RETURNING VARCHAR2(50)));

-- [List<객체> 다중값 인덱스] 교체 부품 번호 배열 인덱스 ★
CREATE MULTIVALUE INDEX idx_json_part_number 
    ON tb_down_event_log d (d.part_replacements.partNumber);

-- [List<객체> 다중값 인덱스] 교체 부품명 배열 인덱스 ★
CREATE MULTIVALUE INDEX idx_json_part_name 
    ON tb_down_event_log d (d.part_replacements.partName);

-- [JSON 통합 검색 인덱스] 부품 교체 상세 내역 풀 텍스트 검색용 Search Index
CREATE SEARCH INDEX idx_down_event_parts_search 
    ON tb_down_event_log (part_replacements) FOR JSON;
	
-- "Valve"라는 텍스트가 어디든 포함되면 검색
-- SELECT * FROM tb_down_event_log
-- WHERE JSON_SEARCH(part_replacements, 'one', '%Valve%') IS NOT NULL;
-- 결과: DOWN001 (partName에 "Valve Assembly" 포함)

-- "PN-002"가 어디든 포함되면 검색
-- SELECT * FROM tb_down_event_log
-- WHERE JSON_SEARCH(part_replacements, 'one', '%PN-002%') IS NOT NULL;
-- 결과: DOWN001 (partNumber에 "PN-002" 포함)

-- 모든 "Pump"를 포함하는 레코드 검색
-- SELECT * FROM tb_down_event_log
-- WHERE JSON_SEARCH(part_replacements, 'all', '%Pump%') IS NOT NULL;	
	
-- 테이블 코멘트 추가
COMMENT ON TABLE tb_down_event_log IS '설비 다운 이벤트 로그 및 상세 정보';

-- 컬럼 코멘트 추가
COMMENT ON COLUMN tb_down_event_log.down_event_id IS '다운 이벤트 고유 ID (PK)';
COMMENT ON COLUMN tb_down_event_log.equipment_id IS '장비 고유 ID';
COMMENT ON COLUMN tb_down_event_log.chamber_id IS '챔버 고유 ID';
COMMENT ON COLUMN tb_down_event_log.fabrication_plant IS 'FAB 코드 (Enum: FabricationPlant)';
COMMENT ON COLUMN tb_down_event_log.process_module IS '공정 모듈 (Enum: ProcessModule)';
COMMENT ON COLUMN tb_down_event_log.equipment_model IS '장비 모델 (Enum: EquipmentModel)';
COMMENT ON COLUMN tb_down_event_log.down_type IS '다운 종류 (Enum: DownType)';
COMMENT ON COLUMN tb_down_event_log.work_status IS '작업 진행 상태 (Enum: WorkStatus)';
COMMENT ON COLUMN tb_down_event_log.down_start_datetime IS '다운 시작 일시 (Epoch ms)';
COMMENT ON COLUMN tb_down_event_log.down_end_datetime IS '다운 복구 일시 (Epoch ms)';
COMMENT ON COLUMN tb_down_event_log.down_duration_minutes IS '다운 지속 시간 (분 단위)';
COMMENT ON COLUMN tb_down_event_log.is_critical IS '심각 다운 여부 (Oracle Native BOOLEAN)';
COMMENT ON COLUMN tb_down_event_log.down_code IS '다운 코드';
COMMENT ON COLUMN tb_down_event_log.down_code_description IS '다운 코드 상세 설명';
COMMENT ON COLUMN tb_down_event_log.alarm_id IS '알람 ID';
COMMENT ON COLUMN tb_down_event_log.assigned_technician IS '담당 엔지니어 정보 (JSON VO 객체: AssignedTechnician)';
COMMENT ON COLUMN tb_down_event_log.approver IS '결재자 정보 (JSON VO 객체: Approver)';
COMMENT ON COLUMN tb_down_event_log.part_replacements IS '교체 부품 목록 (JSON List<PartReplacement> 배열)';
COMMENT ON COLUMN tb_down_event_log.created_by    IS '최초 등록자 사번/ID';
COMMENT ON COLUMN tb_down_event_log.created_at IS '최초 등록 일시';
COMMENT ON COLUMN tb_down_event_log.created_by    IS '최초 등록자 사번/ID';
COMMENT ON COLUMN tb_down_event_log.updated_at IS '최종 수정 일시';	
	
-- =====================================================================	
-- 4. tb_down_content (현상 및 조치 상세 본문 테이블)
-- =====================================================================
CREATE TABLE IF NOT EXISTS tb_down_content (
    down_event_id       VARCHAR2(30)                                    NOT NULL,
    content_html        CLOB                                            NULL,
    created_by          VARCHAR2(50),
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP   NOT NULL,
    updated_by          VARCHAR2(50),
    updated_at          TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP   NOT NULL
)
LOB (content_html) STORE AS SECUREFILE (
    ENABLE STORAGE IN ROW
    CHUNK 8192
    RETENTION
    CACHE
);

-- 2. 기본키(PK) 제약조건 추가
ALTER TABLE tb_down_content
    ADD CONSTRAINT pk_down_content PRIMARY KEY (down_event_id);
-- 3. 외래키(FK) 제약조건 추가 (부모 테이블 삭제 시 자동 연쇄 삭제)
ALTER TABLE tb_down_content
    ADD CONSTRAINT fk_down_content_event 
        FOREIGN KEY (down_event_id) 
        REFERENCES tb_down_event_log (down_event_id) 
        ON DELETE CASCADE;


-- 테이블 코멘트 추가
COMMENT ON TABLE tb_down_content IS '설비 다운 현상 및 조치 상세 Froala HTML 본문 테이블';

-- 컬럼 코멘트 등록
COMMENT ON COLUMN tb_down_content.down_event_id IS '다운 이벤트 고유 ID (PK, FK)';
COMMENT ON COLUMN tb_down_content.content_html  IS 'Froala 에디터 작성 HTML 본문 (이미지 경로/표 포함)';
COMMENT ON COLUMN tb_down_content.created_by    IS '최초 등록자 사번/ID';
COMMENT ON COLUMN tb_down_content.created_at    IS '최초 등록 일시';
COMMENT ON COLUMN tb_down_content.updated_by    IS '최종 수정자 사번/ID';
COMMENT ON COLUMN tb_down_content.updated_at    IS '최종 수정 일시';	

-- =====================================================================	
-- 5. tb_attached_file (통합 첨부파일 및 에디터 인라인 이미지 메타데이터 테이블)
-- =====================================================================
CREATE TABLE IF NOT EXISTS tb_attached_file (
    file_id             VARCHAR2(50)                                    NOT NULL, -- 고유 파일 ID (UUID)
    ref_type            VARCHAR2(30)                                    NOT NULL, -- 참조 도메인 (DOWN_CONTENT_INLINE, DOWN_ATTACHMENT 등)
    ref_id              VARCHAR2(50)                                    NULL,     -- 연관 마스터 ID (down_event_id, 업로드 초기에는 NULL)
    origin_file_name    VARCHAR2(300)                                   NOT NULL, -- 사용자가 올린 원래 파일명
    stored_file_name    VARCHAR2(300)                                   NOT NULL, -- 디스크/S3에 저장된 난수화 파일명 (UUID.ext)
    file_path           VARCHAR2(500)                                   NOT NULL, -- 저장 경로 또는 S3 Object Key
    file_size           NUMBER(19)                                      NOT NULL, -- 파일 크기 (Bytes)
    content_type        VARCHAR2(100)                                   NOT NULL, -- MIME Type (image/png, application/pdf 등)
    file_status         VARCHAR2(20) DEFAULT 'TEMP'                     NOT NULL, -- 상태 (TEMP: 임시, CONFIRMED: 확정, DELETED: 삭제)
    created_by          VARCHAR2(50),
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP   NOT NULL,
    updated_by          VARCHAR2(50),
    updated_at          TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP   NOT NULL
);

-- 기본키(PK) 및 인덱스 추가
ALTER TABLE tb_attached_file ADD CONSTRAINT pk_attached_file PRIMARY KEY (file_id);
CREATE INDEX idx_attached_file_ref ON tb_attached_file (ref_type, ref_id);
CREATE INDEX idx_attached_file_status ON tb_attached_file (file_status, created_at);

-- 코멘트 등록
COMMENT ON TABLE tb_attached_file IS '통합 첨부파일 및 에디터 인라인 이미지 메타데이터 테이블';
COMMENT ON COLUMN tb_attached_file.file_id          IS '파일 고유 ID (UUID)';
COMMENT ON COLUMN tb_attached_file.ref_type         IS '연관 업무 구분 (예: DOWN_CONTENT_INLINE)';
COMMENT ON COLUMN tb_attached_file.ref_id           IS '연관 마스터 ID (down_event_id)';
COMMENT ON COLUMN tb_attached_file.origin_file_name IS '사용자 업로드 원본 파일명';
COMMENT ON COLUMN tb_attached_file.stored_file_name IS '저장소 저장 난수화 파일명 (UUID.ext)';
COMMENT ON COLUMN tb_attached_file.file_path        IS '저장소 상대/절대 경로 또는 S3 Key';
COMMENT ON COLUMN tb_attached_file.file_size        IS '파일 크기 (Byte 단위)';
COMMENT ON COLUMN tb_attached_file.content_type     IS 'MIME 타입 (image/png 등)';
COMMENT ON COLUMN tb_attached_file.file_status      IS '파일 상태 (TEMP / CONFIRMED / DELETED)';
COMMENT ON COLUMN tb_attached_file.created_by       IS '최초 등록자 사번/ID';
COMMENT ON COLUMN tb_attached_file.created_at       IS '최초 등록 일시';
COMMENT ON COLUMN tb_attached_file.updated_by       IS '최종 수정자 사번/ID';
COMMENT ON COLUMN tb_attached_file.updated_at       IS '최종 수정 일시';
	
