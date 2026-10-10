package com.eduguard.eduguard.support;

import com.eduguard.eduguard.student.StudentRepository;
import com.eduguard.eduguard.support.SupportRequests.BarrierRequest;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StudentSupportServiceTest {

    private final StudentRepository studentRepository =
            mock(StudentRepository.class);
    private final SupportBarrierRepository barrierRepository =
            mock(SupportBarrierRepository.class);
    private final FinancialSupportRepository financialRepository =
            mock(FinancialSupportRepository.class);
    private final WellbeingRequestRepository wellbeingRepository =
            mock(WellbeingRequestRepository.class);

    private final StudentSupportService service = new StudentSupportService(
            studentRepository, barrierRepository, financialRepository,
            wellbeingRepository);

    @Test
    void rejectsSupportForUnknownStudent() {
        when(studentRepository.existsById(99L)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getBarriers(99L));

        assertEquals(404, exception.getStatusCode().value());
    }

    @Test
    void rejectsDuplicateActiveBarrierType() {
        when(studentRepository.existsById(1L)).thenReturn(true);
        when(barrierRepository
                .existsByStudentIdAndBarrierTypeAndActiveTrue(
                        1L, "COMMUTING"))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.addBarrier(1L, new BarrierRequest(
                        "COMMUTING", "Long commute", "MEDIUM")));

        assertEquals(409, exception.getStatusCode().value());
    }
}
