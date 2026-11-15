package com.yeopjari.api;

import com.yeopjari.api.Dtos.RoutePreviewRequest;
import com.yeopjari.domain.model.RoutePreview;
import com.yeopjari.domain.service.RouteService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/route")
public class RouteController {

    private final RouteService routes;

    public RouteController(RouteService routes) {
        this.routes = routes;
    }

    /**
     * 출발 전(P단) 길 예습. 차량 앱이 내비에서 받은 빠른 길(+편한 길) 안내 지점 목록을 보내면
     * 긴장 구간 최대 3곳과 "쉬운 길 +N분" 제안을 돌려준다.
     */
    @PostMapping("/preview")
    public RoutePreview preview(@RequestBody RoutePreviewRequest req) {
        if (req.driverId() == null || req.fastRoute() == null) {
            throw new IllegalArgumentException("driverId and fastRoute are required");
        }
        return routes.preview(req.driverId(), req.fastRoute(), req.easyRoute());
    }
}
