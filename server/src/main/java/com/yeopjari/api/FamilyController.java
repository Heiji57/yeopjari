package com.yeopjari.api;

import com.yeopjari.api.Dtos.ShareRequest;
import com.yeopjari.domain.model.Driver;
import com.yeopjari.domain.model.FamilyFeed;
import com.yeopjari.domain.model.SeniorInsight;
import com.yeopjari.domain.model.ShareSetting;
import com.yeopjari.domain.service.FamilyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class FamilyController {

    private final FamilyService family;

    public FamilyController(FamilyService family) {
        this.family = family;
    }

    /** 가족 웹 피드. 운전자가 공유를 켠 항목만, 위치 정보 없이. */
    @GetMapping("/family/{familyId}/feed")
    public FamilyFeed feed(@PathVariable("familyId") String familyId) {
        return family.feed(familyId);
    }

    @GetMapping("/drivers/{driverId}")
    public Driver driver(@PathVariable("driverId") String driverId) {
        return family.driver(driverId);
    }

    @GetMapping("/drivers/{driverId}/share")
    public ShareSetting share(@PathVariable("driverId") String driverId) {
        return family.share(driverId);
    }

    /**
     * 공유 설정 변경. 운전자 본인의 차량 앱(또는 본인 계정)에서만 호출한다.
     * TODO(대회 이후): 로그인 붙이면 요청자 == driverId 인지 검사.
     */
    @PutMapping("/drivers/{driverId}/share")
    public ShareSetting updateShare(@PathVariable("driverId") String driverId, @RequestBody ShareRequest req) {
        return family.updateShare(driverId,
                new ShareSetting(driverId, req.milestones(), req.weeklySummary(), req.arrivalAlert()));
    }

    /** 시니어 모드 차 안 제안 (타이어 경고 → 정비소 경유, 밤 운전 ↑ → 낮 일정). */
    @GetMapping("/drivers/{driverId}/insights")
    public List<SeniorInsight> insights(@PathVariable("driverId") String driverId) {
        return family.insights(driverId);
    }
}
