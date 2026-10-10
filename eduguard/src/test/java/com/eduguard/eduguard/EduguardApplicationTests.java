package com.eduguard.eduguard;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
		"spring.jpa.hibernate.ddl-auto=none",
		"spring.jpa.show-sql=false"
})
class EduguardApplicationTests {

	@Test
	void contextLoads() {
	}

}
