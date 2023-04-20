package hu.koncsik.extension;

import java.time.LocalDateTime;

public class CustomLocalDateTime {
    private LocalDateTime localDateTime;

    public CustomLocalDateTime() {
        this.localDateTime = LocalDateTime.now();
    }

    public CustomLocalDateTime(LocalDateTime localDateTime) {
        this.localDateTime = localDateTime;
    }

    public LocalDateTime getLocalDateTime() {
        return localDateTime;
    }

    public void setLocalDateTime(LocalDateTime localDateTime) {
        this.localDateTime = localDateTime;
    }
    public static CustomLocalDateTime now(){
        return new CustomLocalDateTime(LocalDateTime.now());
    }
}

