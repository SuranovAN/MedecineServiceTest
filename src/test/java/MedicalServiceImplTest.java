import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import ru.netology.patient.entity.BloodPressure;
import ru.netology.patient.entity.HealthInfo;
import ru.netology.patient.entity.PatientInfo;
import ru.netology.patient.repository.PatientInfoFileRepository;
import ru.netology.patient.repository.PatientInfoRepository;
import ru.netology.patient.service.alert.SendAlertService;
import ru.netology.patient.service.alert.SendAlertServiceImpl;
import ru.netology.patient.service.medical.MedicalService;
import ru.netology.patient.service.medical.MedicalServiceImpl;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;

public class MedicalServiceImplTest {
    private static long startTests;
    private long startTestTime;

    @BeforeAll
    static void init() {
        System.out.println("Starting Tests");
        startTests = System.currentTimeMillis();
    }

    @BeforeEach
    void initTest() {
        System.out.println("Start new test");
        startTestTime = System.currentTimeMillis();
    }

    @AfterEach
    void completeTest() {
        System.out.println("\nTest complete " + (System.currentTimeMillis() - startTestTime) + " milliseconds\n");
    }

    @AfterAll
    static void completeTests() {
        System.out.println("Tests complete " + (System.currentTimeMillis() - startTests) + " milliseconds");
    }

    @Mock
    ObjectMapper mapper = new ObjectMapper();
    File repoFile = new File("patients.txt");
    PatientInfoRepository patientInfoRepositoryMock = Mockito.spy(new PatientInfoFileRepository(repoFile, mapper));
    SendAlertService alertServiceMock = Mockito.mock(SendAlertServiceImpl.class);

    @Captor
    ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void checkMessage_WhileBloodPressureTesting() {
        MedicalService medicalService = new MedicalServiceImpl(patientInfoRepositoryMock, alertServiceMock);

        mapper.registerModules(new JavaTimeModule(), new ParameterNamesModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        String id1 = patientInfoRepositoryMock.add(
                new PatientInfo("Иван", "Петров", LocalDate.of(1980, 11, 26),
                        new HealthInfo(new BigDecimal("36.65"), new BloodPressure(120, 80)))
        );
        BloodPressure badPressure = new BloodPressure(60, 120);
        BloodPressure goodPressure = new BloodPressure(120, 80);

        Mockito.doCallRealMethod().when(alertServiceMock).send(Mockito.anyString());

        medicalService.checkBloodPressure(id1, badPressure);
        Mockito.verify(alertServiceMock, Mockito.times(1)).send(Mockito.anyString());
        Mockito.verify(alertServiceMock).send(argumentCaptor.capture());

        PatientInfo patientInfo = patientInfoRepositoryMock.getById(id1);
        String expectedString = String.format("Warning, patient with id: %s, need help", patientInfo.getId());
        Assertions.assertEquals(expectedString, argumentCaptor.getValue());

        medicalService.checkBloodPressure(id1, goodPressure);
       Mockito.verify(alertServiceMock, Mockito.times(1)).send(Mockito.anyString());

    }

    @Test
    void checkMessage_WhileTemperatureTesting() {
        MedicalService medicalService = new MedicalServiceImpl(patientInfoRepositoryMock, alertServiceMock);
        mapper.registerModules(new JavaTimeModule(), new ParameterNamesModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        String id1 = patientInfoRepositoryMock.add(
                new PatientInfo("Семен", "Михайлов", LocalDate.of(1982, 1, 16),
                        new HealthInfo(new BigDecimal("36.6"), new BloodPressure(125, 78)))
        );
        BigDecimal badTemperature = new BigDecimal("34.9");
        BigDecimal goodTemperature = new BigDecimal("36.6");

        medicalService.checkTemperature(id1, badTemperature);
        Mockito.verify(alertServiceMock, Mockito.times(1)).send(Mockito.anyString());
        Mockito.verify(alertServiceMock).send(argumentCaptor.capture());

        medicalService.checkTemperature(id1, goodTemperature);
        Mockito.verify(alertServiceMock, Mockito.times(1)).send(Mockito.anyString());
    }
}
