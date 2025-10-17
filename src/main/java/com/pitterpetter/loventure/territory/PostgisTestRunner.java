package com.pitterpetter.loventure.territory;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
@RequiredArgsConstructor
public class PostgisTestRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        // JSON 파일 경로 (resources 폴더에 위치)
        Path filePath = Path.of("src/main/resources/sgg_seoul_gyeonggi.json");
        if (!Files.exists(filePath)) {
            System.out.println("⚠️ 지역 JSON 파일을 찾을 수 없습니다: " + filePath.toAbsolutePath());
            return;
        }

        // JSON 파일 내용 읽기
        String json = Files.readString(filePath);

        // region 테이블 생성 (없으면)
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS region (
                id VARCHAR(255) PRIMARY KEY,
                sig_cd VARCHAR(10) UNIQUE,
                si_do VARCHAR(30),
                gu_si VARCHAR(30),
                geom geometry(MultiPolygon, 4326)
            );
        """);

        // PostGIS 확장 활성화
        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS postgis;");

        // 실제 GeoJSON → region 삽입
        String sql = """
            WITH fc AS (
              SELECT ?::jsonb AS j
            ),
            features AS (
              SELECT
                COALESCE(
                  f->'properties'->>'sig_cd',
                  f->'properties'->>'SIG_CD'
                ) AS sig_cd,
                COALESCE(
                  f->'properties'->>'name_ko',
                  f->'properties'->>'SIG_KOR_NM'
                ) AS gu_si,
                COALESCE(
                  f->'properties'->>'parent',
                  f->'properties'->>'CTP_KOR_NM'
                ) AS si_do,
                ST_SetSRID(
                  ST_GeomFromGeoJSON(f->>'geometry'),
                  4326
                )::geometry(MultiPolygon,4326) AS geom
              FROM fc, jsonb_array_elements(fc.j->'features') AS f
            )
            INSERT INTO region (id, sig_cd, si_do, gu_si, geom)
            SELECT sig_cd, sig_cd, si_do, gu_si, ST_MakeValid(ST_Multi(geom))
            FROM features
            ON CONFLICT (sig_cd) DO NOTHING;
        """;

        jdbcTemplate.update(sql, json);

        // 검증 로그
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM region", Integer.class);
        System.out.println("✅ 지역 데이터 로드 완료 (" + count + "개 행정구역)");
    }
}
