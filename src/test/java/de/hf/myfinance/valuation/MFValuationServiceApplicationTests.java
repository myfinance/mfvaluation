package de.hf.myfinance.valuation;

import de.hf.myfinance.mfinstrumentclient.MFInstrumentClient;
import de.hf.myfinance.restmodel.Instrument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest(webEnvironment = RANDOM_PORT)
class MFValuationServiceApplicationTests {

	 @Autowired
	 private WebTestClient client;

	@MockBean
	private MFInstrumentClient instrumentClient;

	 private static final int INSTRUMENT_ID_OK = 1;

	@BeforeEach
	void setUp() {

		when(instrumentClient.getInstrument(INSTRUMENT_ID_OK)).
				thenReturn(new Instrument(INSTRUMENT_ID_OK, "name","mock-address"));
	}

	@Test
	void contextLoads() {
	}

	@Test
	void getInstrumentById() {

		client.get()
		.uri("/helloInstrument/" + INSTRUMENT_ID_OK)
		.accept(APPLICATION_JSON)
		.exchange()
		.expectStatus().isOk()
		.expectHeader().contentType(APPLICATION_JSON)
		.expectBody()
			.jsonPath("$.instrumentid").isEqualTo(INSTRUMENT_ID_OK);
	}

	@Test
	void testHelloInstrumentService() {

		client.get()
				.uri("/helloInstrumentService" )
				.accept(APPLICATION_JSON)
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(APPLICATION_JSON)
				.expectBody()
				.jsonPath("$.instrumentid").isEqualTo(INSTRUMENT_ID_OK);
	}

	@Test
	void getException() {

		client.get()
				.uri("/helloException" )
				.accept(APPLICATION_JSON)
				.exchange()
				.expectStatus().is5xxServerError()
				.expectHeader().contentType(APPLICATION_JSON)
				.expectBody()
				.jsonPath("$.path").isEqualTo("/helloException")
				.jsonPath("$.message").isEqualTo("21005:  wrong instrumenttype to calculate positions:");
	}

}
