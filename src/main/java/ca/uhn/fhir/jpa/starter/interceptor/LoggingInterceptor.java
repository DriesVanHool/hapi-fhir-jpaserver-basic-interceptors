package ca.uhn.fhir.jpa.starter.interceptor;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.ResponseDetails;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Interceptor
public class LoggingInterceptor {

	private static final Logger logger = LoggerFactory.getLogger(LoggingInterceptor.class);

	// Create an instance of FhirContext
	private static final FhirContext fhirContext = FhirContext.forR4();

	@Hook(Pointcut.SERVER_INCOMING_REQUEST_POST_PROCESSED)
	public void logIncomingRequest(RequestDetails requestDetails) {
		var reqId = requestDetails.getRequestId();
		var reqType = requestDetails.getRequestType();
		var reqOp = requestDetails.getRestOperationType();
		var reqPath = requestDetails.getRequestPath();
		String reqBody = null;
		ValidationResult validationResult = null;

		if (
			requestDetails instanceof ServletRequestDetails servletRequestDetails &&
				(
					requestDetails.getRequestType() == RequestTypeEnum.PUT
						|| requestDetails.getRequestType() == RequestTypeEnum.POST
				)
		) {
			HttpServletRequest request = servletRequestDetails.getServletRequest();

			try {
				// Read the body from the request's input stream
				byte[] bodyBytes = IOUtils.toByteArray(request.getInputStream());

				// Log the body as a string
				reqBody = new String(bodyBytes, StandardCharsets.UTF_8);
				// Parse the resource from the JSON body
				IBaseResource resource = fhirContext.newJsonParser().parseResource(reqBody);
				// Get a validator instance
				FhirValidator validator = fhirContext.newValidator();
				// Validate the resource
				validationResult = validator.validateWithResult(resource);

				// Replace the original input stream with a new one containing the cached data
				HttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(request, bodyBytes);
				servletRequestDetails.setServletRequest(wrappedRequest);
			} catch (IOException e) {
				logger.error("Error reading request [{}] body", reqId, e);
			}
		}

		logger.info(
			"Incoming request [{}]:\nID: {}\nType: {}\nREST Operation: {}\nPath: /{}\nBody: {}\nValidation: {}",
			reqId, reqId, reqType, reqOp, reqPath,
			reqBody != null ? reqBody : "no body",
			validationResult != null ? validationResult.getMessages() : "no validation"
		);

	}

	private static class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
		private final byte[] body;

		public CachedBodyHttpServletRequest(HttpServletRequest request, byte[] body) {
			super(request);
			this.body = body;
		}

		@Override
		public ServletInputStream getInputStream() {
			ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(body);
			return new ServletInputStreamWrapper(byteArrayInputStream);
		}
	}

	private static class ServletInputStreamWrapper extends ServletInputStream {
		private final ByteArrayInputStream byteArrayInputStream;

		public ServletInputStreamWrapper(ByteArrayInputStream byteArrayInputStream) {
			this.byteArrayInputStream = byteArrayInputStream;
		}

		@Override
		public int read() {
			return byteArrayInputStream.read();
		}

		@Override
		public boolean isFinished() {
			return byteArrayInputStream.available() == 0;
		}

		@Override
		public boolean isReady() {
			return true;
		}

		@Override
		public void setReadListener(ReadListener readListener) {
			// No-op
		}
	}

}
