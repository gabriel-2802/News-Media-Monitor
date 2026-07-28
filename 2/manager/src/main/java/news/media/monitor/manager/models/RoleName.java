package news.media.monitor.manager.models;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RoleName {
    public static final String USER         = "ROLE_USER";
    public static final String ADMIN        = "ROLE_ADMIN";
    public static final String SYSTEM       = "ROLE_SYSTEM";

    public static final String USER_SHORT    = "USER";
    public static final String ADMIN_SHORT   = "ADMIN";
    public static final String SYSTEM_SHORT  = "SYSTEM";
}