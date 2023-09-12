package com.valueplus.app.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.valueplus.app.exception.NotFoundException;
import com.valueplus.app.exception.ValuePlusException;
import com.valueplus.app.model.SocialMediaReportResponse;
import com.valueplus.app.model.SuperAgentFilter;
import com.valueplus.betway.BetWayAgentData;
import com.valueplus.domain.enums.OrderStatus;
import com.valueplus.domain.model.*;
import com.valueplus.domain.model.bet9ja.Bet9jaResponse;
import com.valueplus.domain.products.Bet9jaService;
import com.valueplus.domain.products.ValuePlusProductProvider;
import com.valueplus.domain.products.ValuePlusService;
import com.valueplus.domain.service.abstracts.SocialMediaReportService;
import com.valueplus.domain.service.concretes.*;
import com.valueplus.domain.util.UserUtils;
import com.valueplus.persistence.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;

import static com.valueplus.domain.util.FunctionUtil.toDate;
import static com.valueplus.domain.util.UserUtils.getLoggedInUser;
import static java.time.temporal.TemporalAdjusters.firstDayOfMonth;
import static java.time.temporal.TemporalAdjusters.lastDayOfMonth;
import static org.springframework.data.domain.Sort.Direction.DESC;

@RequiredArgsConstructor
@Validated
@Slf4j
@RestController
@RequestMapping(path = "v1/user", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserController {

    private final UserService userService;
    private final ProfilePictureService profilePictureService;
    private final ActiveAgentService activeAgentService;
    private final ParseRSSFeedUsingCommons parseRSSFeedUsingCommons;
    private final ValuePlusService valuePlusService;
    private final ValuePlusProductProvider productProvider;
    private final SocialMediaReportService socialMediaReportService;
    private final CurrencyConverterService converterService;
    private final Bet9jaService bet9jaService;

    @PreAuthorize("hasAuthority('VIEW_ALL_USERS')")
    @GetMapping
    public Page<AgentDto> findAll(@PageableDefault(sort = "id", direction = DESC) Pageable pageable) {
        return userService.findUsers(pageable)
                .map(u -> AgentDto.valueOf(u, userService.productUrlProvider()));
    }

    @PreAuthorize("hasAuthority('VIEW_ALL_USERS')")
    @GetMapping("/agents-under-admin/{roleId}")
    public Page<AgentDto> getAllAgentsUnderAdmin(@PathVariable Long roleId, @PageableDefault(sort = "id", direction = DESC) Pageable pageable) throws ValuePlusException {
        return userService.findAllAgentsUnderAdmin(roleId, pageable)
                .map(u -> AgentDto.valueOf(u, userService.productUrlProvider()));
    }

    @PreAuthorize("hasAuthority('VIEW_ALL_USERS')")
    @PostMapping("/searches")
    public Page<AgentDto> searchUsers(@Valid @RequestBody UserSearchFilter searchFilter,
                                      @PageableDefault(sort = "id", direction = DESC) Pageable pageable) throws ValuePlusException {
        return userService.searchUsers(searchFilter, pageable);
    }

    @PreAuthorize("hasAuthority('VIEW_SUPER_AGENTS')")
    @GetMapping("/super-agents")
    public Page<AgentDto> findAllSuperAgents(@PageableDefault(sort = "id", direction = DESC) Pageable pageable) {

        return userService.findSuperAgentUsers(pageable)
                .map(u -> AgentDto.valueOf(u, userService.productUrlProvider()));
    }


    @PreAuthorize("hasAnyAuthority('VIEW_SUPER_AGENTS','ROLE_SUPER_AGENT')")
    @GetMapping("/super-agents/{agentCode}/users")
    public Page<AgentDto> getUserBySuperAgentCode(@PathVariable("agentCode") String superAgentCode, @PageableDefault(sort = "id", direction = DESC) Pageable pageable) {
        log.debug("getUser() referralCode = {}", superAgentCode.toLowerCase());

        return userService.findAllUserBySuperAgentCode(superAgentCode.toLowerCase(), pageable)
                .map(u -> AgentDto.valueOf(u, userService.productUrlProvider()));
    }

    @PreAuthorize("hasAnyAuthority('VIEW_AGENTS','ROLE_SUPER_AGENT')")
    @GetMapping("/agents/{agentId}/users")
    public Page<AgentDto> getUserByVpAgentCode(@PathVariable("agentId") Long agentId, @PageableDefault(sort = "id", direction = DESC) Pageable pageable) {
        return userService.findAllUserByVpAgentCode(agentId, pageable)
                .map(u -> AgentDto.valueOf(u, userService.productUrlProvider()));
    }

    @PreAuthorize("hasAnyAuthority('VIEW_SUPER_AGENTS','ROLE_SUPER_AGENT')")
    @PostMapping("/super-agents/filter-active-users")
    public Page<AgentDto> getUserBySuperAgentCode(@Valid @RequestBody SuperAgentFilter superAgentFilter, @PageableDefault(sort = "id", direction = DESC) Pageable pageable) {
        return activeAgentService.getAllActiveSuperAgents(superAgentFilter, pageable);
    }

    @GetMapping("/{userId}")
    public AgentDto getUser(@PathVariable("userId") long userId) {
        log.debug("getUser() id = {}", userId);
        return userService.find(userId)
                .map(u -> AgentDto.valueOf(u, userService.productUrlProvider()))
                .orElseThrow(() -> new NotFoundException("user not found"));
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping(method = RequestMethod.GET, path = "/current")
    public AgentDto getCurrentUser() {
        User user = getLoggedInUser();
        String photo = profilePictureService.getImage(user)
                .orElse(null);

        return AgentDto.valueOf(user, photo, userService.productUrlProvider());
    }
    
    @PreAuthorize("hasAuthority('UPDATE_USER')")
    @PutMapping("/{userId}")
    public AgentDto update(@RequestParam("userId") long userId, @Valid @RequestBody UserUpdate userUpdate) {
        return AgentDto.valueOf(
                userService.update(userUpdate.toUser(userId)),
                userService.productUrlProvider());
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/current")
    public AgentDto update(@Valid @RequestBody UserUpdate userUpdate) {
        long userId = getLoggedInUser().getId();
        return AgentDto.valueOf(
                userService.update(userUpdate.toUser(userId)),
                userService.productUrlProvider());
    }

    @PreAuthorize("hasAuthority('UPDATE_ADMIN_AUTHORITY')")
    @PostMapping("/{userId}/update-authority")
    public AgentDto updateAdminAuthority(@PathVariable("userId") Long userId, @Valid @RequestBody UserAuthorityUpdate authorityUpdate) {
        return AgentDto.valueOf(
                userService.updateAdminAuthority(userId, authorityUpdate.getAuthorities()),
                userService.productUrlProvider());
    }

    @PreAuthorize("hasAuthority('UPDATE_USER_AUTHORITY')")
    @PostMapping("/{userId}/update-user-authority")
    public AgentDto updateUserAuthority(@PathVariable("userId") Long userId, @Valid @RequestBody UserAuthorityUpdate authorityUpdate) {
        return AgentDto.valueOf(
                userService.updateUserAuthority(userId, authorityUpdate.getAuthorities()),
                userService.productUrlProvider());
    }

    @PreAuthorize("hasAuthority('DISABLE_USER')")
    @PostMapping("/{userId}/disable")
    public AgentDto disableUser(@PathVariable("userId") Long userId) throws ValuePlusException {
        return AgentDto.valueOf(
                userService.disableUser(userId),
                userService.productUrlProvider());
    }

    @PreAuthorize("hasAuthority('ENABLE_USER')")
    @PostMapping("/{userId}/enable")
    public AgentDto enableUser(@PathVariable("userId") Long userId) throws ValuePlusException {
        return AgentDto.valueOf(
                userService.enableUser(userId),
                userService.productUrlProvider());
    }

    @PreAuthorize("hasAuthority('UPDATE_ADMIN_AUTHORITY')")
    @GetMapping("/authorities")
    public List<AuthorityModel> getUserAuthorities() {
        return userService.getAllAuthorities();
    }

    @PreAuthorize("hasAuthority('UPDATE_ADMIN_AUTHORITY')")
    @GetMapping("/authorities/ui")
    public List<AuthorityModel> getUIUserAuthorities() {
        return userService.getAllUIAuthorities();
    }

    @PostMapping("/update-pin")
    public AgentDto pinUpdate(@Valid @RequestBody PinUpdate pinUpdate) throws Exception {
        long userId = getLoggedInUser().getId();
        return AgentDto.valueOf(
                userService.pinUpdate(userId, pinUpdate),
                userService.productUrlProvider());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(path = "/{agentId}/suspend-agent")
    public ResponseEntity<MessageResponse> suspendAgent(@PathVariable("agentId") Long agentId) throws Exception {
        return new ResponseEntity<>(userService.suspendAgent(agentId), HttpStatus.OK);
    }


    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(path = "/{userId}/delete-super-agent")
    public ResponseEntity<MessageResponse> deleteSuperAgent(@PathVariable("userId") Long userId) throws Exception {

        return new ResponseEntity<>(userService.deleteSuperAgent(userId), HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(path = "{superAgentId}/suspend-super_agent")
    public ResponseEntity<MessageResponse> suspendSuperAgent(@PathVariable("superAgentId") Long superAgentId) throws Exception {

        return new ResponseEntity<>(userService.suspendAgent(superAgentId), HttpStatus.OK);
    }

    @GetMapping(path = "/active-user-under-super-agent")
    @ResponseStatus(HttpStatus.OK)
    public ActiveUsers getActiveUsersUnderSuperAgent() throws Exception {
        User user = getLoggedInUser();
        return userService.findActiveAgentsUnderSuperAgent(user);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(path = "/active-user-under-super-agent/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public ActiveUsers getActiveUsersUnderSuperAgentII(@PathVariable("userId") Long userId) throws Exception {
        log.info("contoller method");
        return userService.findActiveAgentsUnderSuperAgent(userId);
    }


    @GetMapping(path = "/betwayreport")
    public ResponseEntity<BetWayAgentData> getBetWayReport() {
        User user = getLoggedInUser();
        return new ResponseEntity(parseRSSFeedUsingCommons.getBetWayReport(user), HttpStatus.OK);

    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(path = "/{userId}/delete-sub-admin")
    public ResponseEntity<MessageResponse> deleteSubAdmin(@PathVariable("userId") Long userId) throws Exception {

        return new ResponseEntity<>(userService.deleteSubAdmin(userId), HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(path = "{subAdminId}/suspend-sub-admin")
    public ResponseEntity<MessageResponse> suspendSubAdmin(@PathVariable("subAdminId") Long subAdminId) throws Exception {

        return new ResponseEntity<>(userService.suspendSubAdmin(subAdminId), HttpStatus.OK);

    }
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(path = "{subAdminId}/enable-sub-admin")
    public ResponseEntity<MessageResponse> enableSubAdmin(@PathVariable("subAdminId") Long subAdminId) throws Exception {

        return new ResponseEntity<>(userService.enableSubAdmin(subAdminId), HttpStatus.OK);

    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/sub-admin")
    public Page<AgentDto> findAllSubAdmin(@PageableDefault(sort = "id", direction = DESC) Pageable pageable) {

        return userService.findAllSubAdmin(pageable)
                .map(u -> AgentDto.valueOf(u, userService.productUrlProvider()));
    }

    @GetMapping("/super-agent-summary/{userId}")
    public SuperAgentSummary getSuperAgentSummary(@PathVariable("userId") Long userId) {

        return userService.getSuperAgentRefferalSummary(userId);

    }

    @GetMapping("/valueplus-summary/{userId}")
    public SuperAgentSummary getAgentRefferalSummary(@PathVariable("userId") Long userId) {
        return userService.getAgentRefferalSummary(userId);
    }

    @GetMapping("/super-agent-referrals/{userId}")
    public Page<User.AgentData> getSuperAgentReferralSummary(@PathVariable("userId") Long userId, @PageableDefault(sort = "id", direction = DESC) Pageable pageable) {

        return userService.getAgentsUnderSuperAgentSummary(userId,pageable);

    }

    @PreAuthorize("hasAuthority('SPECIAL_AFFILBASE')")
    @GetMapping("/agent-summary/{userId}")
    public AgentData getIndividualAgentSummary(@PathVariable("userId") Long userId, @PageableDefault(sort = "id", direction = DESC) Pageable pageable) {

        return userService.getIndividualAgentData(userId);

    }

    @GetMapping("/agent-referrals/{userId}")
    public Page<User.subAgentData> getAgentReferral(@PathVariable("userId") Long userId, @PageableDefault(sort = "id", direction = DESC) Pageable pageable) {

         return userService.getAgentsUnderAgentSummary(userId,pageable);

    }

    @GetMapping("/value-plus")
    public ValuePlusService.VpAgentDetails getVpAgentDetails(){
        return valuePlusService.getVpAgentDetails();
    }


    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/platform_info")
    public List<SocialMediaReportResponse> getMediaStatistics(){
        return socialMediaReportService.getMediaStatistics();

    }
    @GetMapping("/agentUnderSuperAgents/search/{keyword}")
    public Page<User> searchAgentsUnderSuperAgents(@PathVariable("keyword") String keyword, @PageableDefault(sort = "id", direction = DESC) Pageable pageable)
    {
        return userService.searchAgentsUnderSuperAgents(keyword,pageable);
    }

    @GetMapping("/filter-users")
    @ResponseStatus(HttpStatus.OK)
    public Page<User> searchUser(@RequestParam(value = "city", required = false) String city,
                                              @RequestParam(value = "active", required = false) String enabled,
                                                 @PageableDefault(sort = "id", direction = DESC) Pageable pageable) throws ValuePlusException {

        return userService.filterUser(
                city,
                enabled,
                pageable);

    }
    @GetMapping("/filter-active-users/affilbase")
    @ResponseStatus(HttpStatus.OK)
    public Page<AgentDto> searchActiveUser(
                                 @RequestParam(value = "enabled") Boolean enabled,
                                 @PageableDefault(sort = "id", direction = DESC) Pageable pageable) throws ValuePlusException {

        return userService.filterActiveAgentsUnderSuperAgent(enabled,pageable)
                .map(u -> AgentDto.valueOf(u, userService.productUrlProvider()));

    }

    @GetMapping("/convert")
    public String downB (){

        return converterService.convertBetwayEarnings("20");
    }


    @GetMapping("/result")
    public Bet9jaResponse results (){
        LocalDate initial = LocalDate.now();
        LocalDate start = initial.with(firstDayOfMonth());
        LocalDate end = initial.with(lastDayOfMonth());
        return bet9jaService.getAgentReportData(start.toString(),end.toString());
    }




}
