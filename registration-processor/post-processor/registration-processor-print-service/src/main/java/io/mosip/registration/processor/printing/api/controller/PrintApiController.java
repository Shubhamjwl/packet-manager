package io.mosip.registration.processor.printing.api.controller;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.Objects;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.mosip.kernel.core.idvalidator.exception.InvalidIDException;
import io.mosip.kernel.core.idvalidator.spi.RidValidator;
import io.mosip.kernel.core.idvalidator.spi.UinValidator;
import io.mosip.registration.processor.core.constant.IdType;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.spi.print.service.PrintService;
import io.mosip.registration.processor.print.service.exception.RegPrintAppException;
import io.mosip.registration.processor.printing.api.dto.PrintRequest;
import io.mosip.registration.processor.printing.api.dto.RequestDTO;
import io.mosip.registration.processor.printing.api.util.PrintServiceRequestValidator;
import io.mosip.registration.processor.printing.api.util.PrintServiceValidationUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import springfox.documentation.annotations.ApiIgnore;

/**
 * The Class PrintApiController.
 * 
 * @author M1048358 Alok
 */
@RestController
@Api(tags = "Print PDF")
public class PrintApiController {

	/** The printservice. */
	@Autowired
	private PrintService<Map<String, byte[]>> printservice;

	private static final String ID_VALUE = "idValue";

	@Autowired
	private PrintServiceRequestValidator validator;

	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.addValidators(validator);
	}

	/** The rid validator. */
	@Autowired
	private RidValidator<String> ridValidator;

	@Autowired
	private UinValidator<String> uinValidatorImpl;

	/**
	 * Gets the file.
	 *
	 * @param printRequest
	 *            the print request DTO
	 * @return the file
	 * @throws RegPrintAppException
	 */
	@PreAuthorize("hasAnyRole('REGISTRATION_ADMIN')")
	@PostMapping(path = "/uincard", produces = "application/json")
	@ApiOperation(value = "Service to get Pdf of UIN Card", response = Object.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "UIN card is successfully fetched") })
	public ResponseEntity<Object> getFile(@Valid @RequestBody PrintRequest printRequest, @ApiIgnore Errors errors)
			throws RegPrintAppException {

		validateRequest(printRequest.getRequest(), errors);
		PrintServiceValidationUtil.validate(errors);

		byte[] pdfbytes = printservice
				.getDocuments(printRequest.getRequest().getIdtype(), printRequest.getRequest().getIdValue())
				.get("uinPdf");

		InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(pdfbytes));
		return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/pdf"))
				.header("Content-Disposition",
						"attachment; filename=\"" + printRequest.getRequest().getIdValue() + ".pdf\"")
				.body((Object) resource);

	}

	private void validateRequest(RequestDTO dto, Errors errors) throws RegPrintAppException {
		if (!errors.hasErrors()) {
			if (Objects.isNull(dto)) {
				throw new RegPrintAppException(PlatformErrorMessages.RPR_PGS_MISSING_INPUT_PARAMETER.getCode(),
						String.format(PlatformErrorMessages.RPR_PGS_MISSING_INPUT_PARAMETER.getMessage(), "request"));
			} else if (Objects.isNull(dto.getIdtype())) {
				throw new RegPrintAppException(PlatformErrorMessages.RPR_PGS_MISSING_INPUT_PARAMETER.getCode(),
						String.format(PlatformErrorMessages.RPR_PGS_MISSING_INPUT_PARAMETER.getMessage(), "idType"));
			} else if (Objects.isNull(dto.getIdValue())) {
				throw new RegPrintAppException(PlatformErrorMessages.RPR_PGS_MISSING_INPUT_PARAMETER.getCode(),
						String.format(PlatformErrorMessages.RPR_PGS_MISSING_INPUT_PARAMETER.getMessage(), ID_VALUE));
			} else if (dto.getIdtype().equals(IdType.RID)) {
				try {
					ridValidator.validateId(dto.getIdValue());
				} catch (InvalidIDException ex) {
					throw new RegPrintAppException(PlatformErrorMessages.RPR_PRT_DATA_VALIDATION_FAILED.getCode(),
							ex.getMessage());
				}
			} else if (dto.getIdtype().equals(IdType.UIN)) {
				try {
					uinValidatorImpl.validateId(dto.getIdValue());
				} catch (InvalidIDException ex) {
					throw new RegPrintAppException(PlatformErrorMessages.RPR_PRT_DATA_VALIDATION_FAILED.getCode(),
							ex.getMessage());
				}
			}
		}
	}

}
