package app.demo.mappers;

import app.demo.dto.RegisterDTO;
import app.demo.dto.UserDTO;
import app.demo.dto.UserProfileDTO;
import app.demo.entities.Notification;
import app.demo.entities.Role;
import app.demo.entities.Topic;
import app.demo.entities.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(
        componentModel = "spring",
        uses = {RoleMapper.class, NotificationMapper.class}
)
public interface UserMapper {

    @Mapping(target = "subscribedTopics", expression = "java(mapSubscribedTopics(user))")
    @Mapping(target = "roles", expression = "java(mapRoles(user))")
    @BeanMapping(ignoreByDefault = false)
    UserDTO toDTO(User user);

    @Mapping(target = "roles", expression = "java(mapRoles(user))")
    @Mapping(target = "subscribedTopics", expression = "java(mapSubscribedTopics(user))")
    @Mapping(source = "ns", target = "notifications")
    UserProfileDTO toUserProfileDTO(User user, List<Notification> ns);

    List<UserDTO> toDTO(List<User> users);

    User toEntity(RegisterDTO userDTO);

    default Set<String> mapSubscribedTopics(User user) {
        return user.getSubscribedTopics()
                .stream()
                .map(Topic::getName)
                .collect(Collectors.toSet());
    }

    default Set<String> mapRoles(User user) {
        return user.getRoles()
                .stream()
                .map(Role::getAuthority)
                .collect(Collectors.toSet());
    }
}
