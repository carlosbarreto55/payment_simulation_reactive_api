package checkout.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequestDto {

    @NotBlank
    @Email
    private String email;

    @Size(min = 5, max = 255)
    @NotBlank
    private String password;

}
