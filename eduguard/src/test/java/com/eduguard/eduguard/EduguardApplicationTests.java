package com.eduguard.eduguard;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
		"spring.datasource.url=jdbc:h2:mem:eduguard-test;DB_CLOSE_DELAY=-1",
		"spring.datasource.username=sa",
		"spring.jpa.hibernate.ddl-auto=none",
		"spring.jpa.show-sql=false"
})
class EduguardApplicationTests {

	@Test
	void contextLoads() {
	}

}
