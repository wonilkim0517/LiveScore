package ac.su.suport.livescore.controller;

import ac.su.suport.livescore.constant.DepartmentEnum;
import ac.su.suport.livescore.service.DepartmentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    public List<DepartmentEnum> getAllDepartments() {
        return departmentService.getAllDepartments();
    }

}
