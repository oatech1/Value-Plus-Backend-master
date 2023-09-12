package com.valueplus.persistence.repository;

import com.valueplus.persistence.entity.Role;
import com.valueplus.persistence.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Transactional
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmailAndDeletedFalse(String username);

    List<User> findUsersByDeletedFalse();

    List<User>findUsersByEnabledFalse();

    List<User>findUsersByActivatedFalseAndRole(Role role);

    @Query("SELECT u from User u where u.agentCode is null and u.referralCode is null and u.role =?1")
    List<User>findUsersByAgentCodeIsNullAndRole(Role role);




    Page<User> findUsersByDeletedFalse(Pageable pageable);

    Optional<User> findByIdAndDeletedFalse(long userId);

    Page<User> findAllByRoleId(Long roleId, Pageable pageable);

    Optional<User> findByAgentCodeAndDeletedFalseAndRole(String agentCode,Role role);

    @Modifying
    @Query("update User u set u.deleted = true where u.id = ?1")
    void deleteUser(Long userId);

    @Modifying
    @Query("update User u set u.deleted = true, u.enabled = false where u.id = ?1")
    int deleteSuperAgent(Long userId);

    @Query(value = "SELECT u from User u where " +
            "u IN (select p.user from ProductOrder p where " +
            "p.createdAt>=?1 " +
            "AND p.createdAt<=?2 " +
            "AND p.status='COMPLETED' " +
            "AND p.user.superAgent.referralCode=?3)")
    Page<User> findActiveSuperAgentUsers(LocalDateTime startDate, LocalDateTime endDate, String superAgentCode, Pageable pageable);
//
//    @Query(value = "SELECT COUNT (u.id) from User u where " +
//            "u.id =?1" +
//            " AND u IN (select p.user from ProductOrder p where " +
//            "p.status='COMPLETED')")
//    Integer findActiveVpAgent(Long id);

    @Query(value = "SELECT b from User b where exists (SELECT u from User u where " +
            "u.id =?1" +
            " AND u IN (select p.user from ProductOrder p where " +
            "p.status='COMPLETED') OR u IN (select r.count from ReferralCounter r where r.referralCode = u.referralCode) OR u IN ( select b.activeReferrals from BetWayAgentData b where b.user = u))")
    Boolean findActiveVpAgent(Long id);

    @Query(value = "select COUNT (p.user) from ProductOrder p where " +
            "p.status='COMPLETED' and p.user.id =?1")
    Integer findMadeProductOrderSuccessful(Long id);

    @Query(value = " select b.activeReferrals from BetWayAgentData b where b.user = ?1")
    Integer findActiveBetway(User user);


    @Query(value = "select r.count from ReferralCounter r where r.referralCode = ?1")
    Integer findActiveVp(String code);

    @Query(value = "select u from User u where u.deleted = false and u.superAgent.agentCode = :superAgent and u.role.id =2 and " +
            "( (lower(u.firstname) =:searchString) or (lower(u.lastname) =:searchString) or" +
            "((lower(u.firstname)) LIKE %:searchString%) OR " +
            "((lower(u.lastname)) LIKE %:searchString% ))")
    Page<User>searchForAgentUnderSuperAgents(String superAgent,String searchString, Pageable pageable );



    List<User> findUsersBySuperAgent(User superAgent);

    List<User>findUsersByAgent(User user);

    Page<User>findAllByAgent(User user,Pageable pageable);

    Page<User>findAllBySuperAgentAndDeletedFalse(User user, Pageable pageable);

    Page<User.AgentData>findAllBySuperAgent(User superAgent, Pageable pageable);

    Page<User.subAgentData>findUsersByAgent(User agent,Pageable pageable);


    @Query(value = " SELECT COUNT (u.id) from User u where u.agent =?1 ")
    Integer countAgentUnderVpAgent(User vpAgent);

    @Query(value = " SELECT u from User u where u.superAgent =?1 ")
    List<User>getUsersUnderAgent(User user);

    @Query(value = "SELECT u from User u where " +
            "u IN (select p.user from ProductOrder p where " +
            "p.createdAt>=?1 " +
            "AND p.createdAt<=?2 " +
            "AND p.status='COMPLETED' " +
            "AND p.user.superAgent.referralCode=?3)")
    List<User> findActiveSuperAgentListUsers(LocalDateTime startDate, LocalDateTime endDate, String superAgentCode);

    Optional<User> findByAgentCodeAndDeletedFalse(String agentCode);

    Optional<User> findByReferralCode(String referralCode);

    Optional<User> findByReferralCodeAndRole(String referralCode, Role role);

    Page<User> findUserBySuperAgent_ReferralCode(String superAgentCode, Pageable pageable);

    Page<User> findUserByAgent_Id(Long agentId, Pageable pageable);

    Page<User> findUserByRole_NameAndDeletedFalse(String role, Pageable pageable);

    Page<User> findUserByRole_Name(String role, Pageable pageable);

    @Query(value = "SELECT U FROM User U where U.role.name= ?1 and U.deleted =?2 ")
    Page<User>findAllSub_Admin(String role,boolean deleteStatus,Pageable pageable);

    List<User> findUserByRole_Name(String role);

    Long countUserByRole_NameInAndDeleted(List<String> role, Boolean status);

    List<User> findUserBySuperAgent(User superAgent);

    List<User> findUsersBySuperAgent_ReferralCode(String referralCode);

    void deleteUserBySuperAgent(User superAgent);

    Optional<User> findByEmail(String email);

    Optional<User>findById(Long id);

    int countUserByActivatedFalse();

    int countUserByActivatedTrue();
}
