package com.javaweb.controller.admin;



import com.javaweb.constant.SystemConstant;
import com.javaweb.enums.buildingType;
import com.javaweb.enums.districtCode;
import com.javaweb.model.dto.BuildingDTO;
import com.javaweb.model.request.BuildingSearchRequest;
import com.javaweb.model.response.BuildingSearchResponse;
import com.javaweb.security.utils.SecurityUtils;
import com.javaweb.service.IBuildingService;
import com.javaweb.service.IUserService;
import com.javaweb.utils.DisplayTagUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

@Controller(value="buildingControllerOfAdmin")
public class BuildingController {

    private static final Logger log = LoggerFactory.getLogger(BuildingController.class);

    @Autowired
    private IUserService iUserService;
    @Autowired
    private IBuildingService iBuildingService;
    @GetMapping(value = "/admin/building-list")
    public ModelAndView buildingList(@ModelAttribute (SystemConstant.MODEL) BuildingSearchRequest buildingSearchRequest, HttpServletRequest request){
        ModelAndView mav = new ModelAndView("admin/building/list");
        mav.addObject("modelSearch",buildingSearchRequest);
        DisplayTagUtils.of(request,  buildingSearchRequest);
        long start = System.currentTimeMillis();
        List<BuildingSearchResponse> responseList = iBuildingService.findAll(buildingSearchRequest,
                PageRequest.of(buildingSearchRequest.getPage() - 1, buildingSearchRequest.getMaxPageItems()));
        log.info("[SEARCH] findAll mất {} ms", System.currentTimeMillis() - start);
        buildingSearchRequest.setListResult(responseList);
        buildingSearchRequest.setTotalItems(iBuildingService.countTotalItem());
        mav.addObject("buildingList", buildingSearchRequest);
        mav.addObject("listStaffs",iUserService.getStaffs());
        mav.addObject("districts",districtCode.type());
        mav.addObject("typeCodes",buildingType.type());

        return mav;
    }
    @GetMapping(value = "/admin/building-edit")
    public ModelAndView buildingEdit(@ModelAttribute ("buildingEdit") BuildingDTO buildingDTO, HttpServletRequest request){
        ModelAndView mav = new ModelAndView("admin/building/edit");
//        mav.addObject("building", buildingDTO);
        mav.addObject("districts", districtCode.type());
        mav.addObject("typeCodes", buildingType.type());
        return mav;
    }
    @RequestMapping(value = "/admin/building-edit-{id}",method = RequestMethod.GET)
    public ModelAndView buildingEdit(@PathVariable("id") Long id, HttpServletRequest request){
        ModelAndView mav = new ModelAndView("admin/building/edit");
        BuildingDTO buildingDTO = iBuildingService.findBuildingById(id);
        mav.addObject("buildingEdit",buildingDTO);
        mav.addObject("districts", districtCode.type());
        mav.addObject("typeCodes", buildingType.type());
//        mav.addObject("building", buildingDTO);
        return mav;
    }
}
