package com.valueplus.app.controller;

import com.valueplus.app.exception.ValuePlusException;
import com.valueplus.domain.model.*;
import com.valueplus.domain.products.ValuePlusService;
import com.valueplus.domain.service.concretes.RegistrationService;
import com.valueplus.persistence.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.util.List;

import static com.valueplus.domain.model.RoleType.ADMIN;
import static com.valueplus.domain.model.RoleType.SUB_ADMIN;
import static com.valueplus.domain.util.UserUtils.getLoggedInUser;
import static org.springframework.http.HttpStatus.CREATED;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping(path = "v1/register", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
public class RegistrationController {

    private final RegistrationService registrationService;
    private final ValuePlusService  valuePlusService;

    @PreAuthorize("permitAll()")
    @PostMapping
    @ResponseStatus(CREATED)
    public AgentDto register(@Valid @RequestBody AgentCreate agentCreate) throws Exception {
        User registered = registrationService.createAgent(agentCreate);
        return AgentDto.valueOf(registered, registrationService.productUrlProvider());
    }

    @GetMapping("/betway")
    @ResponseStatus(CREATED)
    public MessageResponse registerBetWay() throws ValuePlusException {
        return registrationService.registerBetWay();
    }

//    @GetMapping("/data4me")
//    @ResponseStatus(CREATED)
//    public MessageResponse registerData4me() throws ValuePlusException {
//        return registrationService.registerData4me();
//    }

    @GetMapping("/betacare")
    @ResponseStatus(CREATED)
    public MessageResponse registerBetaCare() throws ValuePlusException {
        return registrationService.registerBetaCare();
    }

    @GetMapping("/valueplus")
    @ResponseStatus(CREATED)
    public MessageResponse registerValuePlus()throws ValuePlusException{
        User user = getLoggedInUser();
        return registrationService.registerValuePlus(user);
    }

    @PreAuthorize("hasAuthority('CREATE_ADMIN')")
    @PostMapping("/admin")
    @ResponseStatus(CREATED)
    public UserDto registerAdmin(@Valid @RequestBody UserCreate userCreate) throws Exception {
        User registered = registrationService.createAdmin(userCreate, ADMIN);
        return UserDto.valueOf(registered);
    }
    @PreAuthorize("hasAuthority('CREATE_ADMIN')")
    @PostMapping("/sub-admin")
    @ResponseStatus(CREATED)
    public UserDto registerSubAdmin(@Valid @RequestBody UserCreate userCreate) throws Exception {
        User registered = registrationService.createSubAdmin(userCreate, SUB_ADMIN);
        return UserDto.valueOf(registered);
    }

    @PreAuthorize("hasAuthority('CREATE_SUPER_AGENT')")
    @PostMapping("/super-agent")
    @ResponseStatus(CREATED)
    public UserDto registerSuperAgent(@Valid @RequestBody UserCreate userCreate) throws Exception {
        User registered = registrationService.createSuperAgent(userCreate);
        return UserDto.valueOf(registered);
    }


//    @PreAuthorize("hasAuthority('CREATE_SUPER_AGENT')")
    @PostMapping("/agent-by-super-agent")
    @ResponseStatus(CREATED)
    public UserDto registerAgentBySuperAgent(@Valid @RequestBody UserCreate userCreate) throws Exception {
        User registered = registrationService.createAgentBySuperAgent(userCreate);
        return UserDto.valueOf(registered);
    }


    @PreAuthorize("permitAll()")
    @PostMapping("/integrate")
    @ResponseStatus(CREATED)
    public List<String> integrateAgent(@Valid @RequestBody AgentCreate agentCreate) throws Exception {
        System.out.println("Entered here");
        return registrationService.integrateAgent(agentCreate);
    }
}
