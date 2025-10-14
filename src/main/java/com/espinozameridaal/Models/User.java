package com.espinozameridaal;

import lombok.*;
import java.time.LocalDate;


@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor

public class User {
    long UserID;
    String UserName;
    String UserPassword;
}
