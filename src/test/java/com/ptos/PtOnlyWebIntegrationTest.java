package com.ptos;

import com.ptos.domain.Role;
import com.ptos.domain.User;
import com.ptos.domain.WorkoutAssignment;
import com.ptos.repository.CheckInRepository;
import com.ptos.repository.ClientRecordRepository;
import com.ptos.repository.MealPlanRepository;
import com.ptos.repository.UserRepository;
import com.ptos.repository.WorkoutAssignmentRepository;
import com.ptos.repository.WorkoutRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest
@AutoConfigureMockMvc
class PtOnlyWebIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkoutRepository workoutRepository;

    @Autowired
    private WorkoutAssignmentRepository workoutAssignmentRepository;

    @Autowired
    private ClientRecordRepository clientRecordRepository;

    @Autowired
    private MealPlanRepository mealPlanRepository;

    @Autowired
    private CheckInRepository checkInRepository;

    @Test
    void publicPagesExposeOnlyPtEntryPoints() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("PT Login")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("PT Sign Up")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Client Sign Up"))));

        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("New client? Sign up here"))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Need a PT account? Sign up here")));

        mockMvc.perform(get("/pt/signup"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Are you a client? Sign up here"))));
    }

    @Test
    void removedClientFacingRoutesReturnNotFound() throws Exception {
        mockMvc.perform(get("/signup"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/client/home"))
                .andExpect(status().isNotFound());
    }

    @Test
    void inviteLinksRemainPublicAndShowInvalidMessageWhenTokenIsUnknown() throws Exception {
        mockMvc.perform(get("/invite/anytoken"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("This invitation link is invalid.")));
    }

    @Test
    void ptLoginRedirectsToDashboardAndDashboardShowsPtNavigation() throws Exception {
        MvcResult loginResult = mockMvc.perform(SecurityMockMvcRequestBuilders.formLogin("/login")
                        .user("pt@ptos.local")
                        .password("password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pt/dashboard"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mockMvc.perform(get("/pt/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Dashboard")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Clients")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Workouts")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Check-ins")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Messages")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Business")));
    }

    @Test
    void createdWorkoutAppearsInMyWorkouts() throws Exception {
        MvcResult loginResult = mockMvc.perform(SecurityMockMvcRequestBuilders.formLogin("/login")
                        .user("pt@ptos.local")
                        .password("password123"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mockMvc.perform(post("/pt/workouts")
                        .session(session)
                        .with(csrf())
                        .param("name", "Lower Body Strength")
                        .param("category", "STRENGTH")
                        .param("description", "Primary lower session")
                        .param("template", "true")
                        .param("exercises[0].exerciseName", "Back Squat")
                        .param("exercises[0].setsCount", "4")
                        .param("exercises[0].repsText", "6")
                        .param("exercises[0].notes", "")
                        .param("exercises[1].exerciseName", "")
                        .param("exercises[1].setsCount", "")
                        .param("exercises[1].repsText", "")
                        .param("exercises[1].notes", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pt/workouts"));

        mockMvc.perform(get("/pt/workouts").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Lower Body Strength")));
    }

    @Test
    void workoutFormIncludesExerciseRowJavascript() throws Exception {
        MvcResult loginResult = mockMvc.perform(SecurityMockMvcRequestBuilders.formLogin("/login")
                        .user("pt@ptos.local")
                        .password("password123"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mockMvc.perform(get("/pt/workouts/new").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("window.addExercise = function")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("window.removeExercise = function")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("onclick=\"addExercise()\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("onclick=\"removeExercise(this)\"")));
    }

    @Test
    void ptCanDeleteWorkoutFromMyWorkoutsPage() throws Exception {
        MvcResult loginResult = mockMvc.perform(SecurityMockMvcRequestBuilders.formLogin("/login")
                        .user("pt@ptos.local")
                        .password("password123"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mockMvc.perform(post("/pt/workouts")
                        .session(session)
                        .with(csrf())
                        .param("name", "Delete Me Workout")
                        .param("category", "STRENGTH")
                        .param("description", "Temporary test workout")
                        .param("template", "true")
                        .param("exercises[0].exerciseName", "Bench Press")
                        .param("exercises[0].setsCount", "4")
                        .param("exercises[0].repsText", "8")
                        .param("exercises[0].notes", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pt/workouts"));

        User ptUser = userRepository.findByEmail("pt@ptos.local").orElseThrow();
        Long workoutId = workoutRepository.findByPtUserOrderByCreatedAtDesc(ptUser).stream()
                .filter(workout -> "Delete Me Workout".equals(workout.getName()))
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(post("/pt/workouts/{id}/delete", workoutId)
                        .session(session)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pt/workouts"));

        assertThat(workoutRepository.findById(workoutId)).isEmpty();

        mockMvc.perform(get("/pt/workouts").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Delete Me Workout"))));
    }

    @Test
    void ptCanUnassignWorkoutFromClient() throws Exception {
        MvcResult loginResult = mockMvc.perform(SecurityMockMvcRequestBuilders.formLogin("/login")
                        .user("pt@ptos.local")
                        .password("password123"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mockMvc.perform(post("/pt/workouts")
                        .session(session)
                        .with(csrf())
                        .param("name", "Unassign Me Workout")
                        .param("category", "STRENGTH")
                        .param("description", "Assignment removal test workout")
                        .param("template", "true")
                        .param("exercises[0].exerciseName", "Deadlift")
                        .param("exercises[0].setsCount", "3")
                        .param("exercises[0].repsText", "5")
                        .param("exercises[0].notes", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pt/workouts"));

        User ptUser = userRepository.findByEmail("pt@ptos.local").orElseThrow();
        User clientUser = userRepository.findByEmail("alex@ptos.local").orElseThrow();
        Long workoutId = workoutRepository.findByPtUserOrderByCreatedAtDesc(ptUser).stream()
                .filter(workout -> "Unassign Me Workout".equals(workout.getName()))
                .findFirst()
                .orElseThrow()
                .getId();
        Long clientRecordId = clientRecordRepository.findByPtUserAndClientUser(ptUser, clientUser)
                .orElseThrow()
                .getId();

        mockMvc.perform(post("/pt/workouts/{id}/assign", workoutId)
                        .session(session)
                        .with(csrf())
                        .param("clientRecordId", String.valueOf(clientRecordId))
                        .param("assignedDate", "2026-04-03"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pt/clients/" + clientRecordId));

        WorkoutAssignment assignment = workoutAssignmentRepository.findByClientRecordOrderByAssignedDateDesc(
                        clientRecordRepository.findById(clientRecordId).orElseThrow())
                .stream()
                .filter(candidate -> "Unassign Me Workout".equals(candidate.getWorkout().getName()))
                .findFirst()
                .orElseThrow();

        mockMvc.perform(post("/pt/workouts/assignments/{id}/delete", assignment.getId())
                        .session(session)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pt/clients/" + clientRecordId));

        assertThat(workoutAssignmentRepository.findById(assignment.getId())).isEmpty();

        mockMvc.perform(get("/pt/clients/{id}", clientRecordId).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Unassign Me Workout"))));
    }

    @Test
    void ptCanDeleteMealPlanForClient() throws Exception {
        MvcResult loginResult = mockMvc.perform(SecurityMockMvcRequestBuilders.formLogin("/login")
                        .user("pt@ptos.local")
                        .password("password123"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();

        User ptUser = userRepository.findByEmail("pt@ptos.local").orElseThrow();
        User clientUser = userRepository.findByEmail("alex@ptos.local").orElseThrow();
        Long clientRecordId = clientRecordRepository.findByPtUserAndClientUser(ptUser, clientUser)
                .orElseThrow()
                .getId();

        mockMvc.perform(post("/pt/clients/{id}/nutrition", clientRecordId)
                        .session(session)
                        .with(csrf())
                        .param("title", "Cutting Plan")
                        .param("overview", "High protein")
                        .param("dailyGuidance", "Breakfast: eggs\nLunch: chicken and rice"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pt/clients/" + clientRecordId + "/nutrition"));

        assertThat(mealPlanRepository.findByClientRecordAndActiveTrue(
                clientRecordRepository.findById(clientRecordId).orElseThrow())).isPresent();

        mockMvc.perform(post("/pt/clients/{id}/nutrition/delete", clientRecordId)
                        .session(session)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pt/clients/" + clientRecordId + "/nutrition"));

        assertThat(mealPlanRepository.findByClientRecordAndActiveTrue(
                clientRecordRepository.findById(clientRecordId).orElseThrow())).isEmpty();

        mockMvc.perform(get("/pt/clients/{id}/nutrition", clientRecordId).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("No meal plan assigned.")));
    }

    @Test
    void jamieChenReviewPageShowsSeededProgressPhoto() throws Exception {
        MvcResult loginResult = mockMvc.perform(SecurityMockMvcRequestBuilders.formLogin("/login")
                        .user("pt@ptos.local")
                        .password("password123"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();

        User jamie = userRepository.findByEmail("jamie@ptos.local").orElseThrow();
        Long latestCheckInId = checkInRepository.findByClientRecord_ClientUserOrderBySubmittedAtDesc(jamie).stream()
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(get("/pt/checkins/{id}", latestCheckInId).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Review Check-in - Jamie Chen")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/uploads/photos/checkins/jamie-chen-front.svg")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/uploads/photos/checkins/jamie-chen-side.svg")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/uploads/photos/checkins/jamie-chen-back.svg")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/uploads/photos/checkins/jamie-chen-front-previous.svg")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Previous (")));
    }

    @Test
    void clientsPageShowsSeededInvitationStatuses() throws Exception {
        MvcResult loginResult = mockMvc.perform(SecurityMockMvcRequestBuilders.formLogin("/login")
                        .user("pt@ptos.local")
                        .password("password123"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mockMvc.perform(get("/pt/clients").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Client Invitations")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Layla Green")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Daniel Foster")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Priya Shah")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("PENDING")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ACCEPTED")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("EXPIRED")));
    }

    @Test
    void alexClientPagesShowSeededMealPlanAndPtNotes() throws Exception {
        MvcResult loginResult = mockMvc.perform(SecurityMockMvcRequestBuilders.formLogin("/login")
                        .user("pt@ptos.local")
                        .password("password123"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();

        User ptUser = userRepository.findByEmail("pt@ptos.local").orElseThrow();
        User alex = userRepository.findByEmail("alex@ptos.local").orElseThrow();
        Long clientRecordId = clientRecordRepository.findByPtUserAndClientUser(ptUser, alex)
                .orElseThrow()
                .getId();

        mockMvc.perform(get("/pt/clients/{id}", clientRecordId).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("PT Note History")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Travel week handled well.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Lean Cut Plan")));

        mockMvc.perform(get("/pt/clients/{id}/nutrition", clientRecordId).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Lean Cut Plan")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Meals were easy to prep.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("followed in last 14 days")));
    }

    @Test
    void clientDetailShowsStartConversationForClientWithoutExistingThread() throws Exception {
        MvcResult loginResult = mockMvc.perform(SecurityMockMvcRequestBuilders.formLogin("/login")
                        .user("pt@ptos.local")
                        .password("password123"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();

        User ptUser = userRepository.findByEmail("pt@ptos.local").orElseThrow();
        User ben = userRepository.findByEmail("ben@ptos.local").orElseThrow();
        Long clientRecordId = clientRecordRepository.findByPtUserAndClientUser(ptUser, ben)
                .orElseThrow()
                .getId();

        mockMvc.perform(get("/pt/clients/{id}", clientRecordId).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Start Conversation")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/pt/messages/new/" + clientRecordId)));
    }

    @Test
    void clientUsersRemainInDataButHaveNoWebDestination() throws Exception {
        assertThat(userRepository.findByEmail("alex@ptos.local"))
                .isPresent()
                .get()
                .extracting(user -> user.getRole())
                .isEqualTo(Role.CLIENT);

        mockMvc.perform(SecurityMockMvcRequestBuilders.formLogin("/login")
                        .user("alex@ptos.local")
                        .password("password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?clientAccess=true"));
    }
}
