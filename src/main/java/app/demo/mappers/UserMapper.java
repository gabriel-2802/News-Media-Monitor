package app.demo.mappers;

import app.demo.dto.RegisterDTO;
import app.demo.dto.UserDTO;
import app.demo.entities.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import app.demo.entities.Topic;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring", uses = {RoleMapper.class})
public interface UserMapper {
    @Mapping(target = "subscribedTopics", expression = "java(mapSubscribedTopics(user))")
    @Mapping(target = "roles", expression = "java(mapRoles(user))")
    @BeanMapping(ignoreByDefault = false)
    UserDTO toDTO(User user);

    List<UserDTO> toDTO(List<User> users);

    User toEntity(RegisterDTO userDTO);

    default Set<String> mapSubscribedTopics(User user) {
        return user.getSubscribedTopics().stream().map(Topic::getName).collect(java.util.stream.Collectors.toSet());
    }

    default Set<String> mapRoles(User user) {
        return user.getRoles().stream().map(role -> role.getAuthority().toString()).collect(java.util.stream.Collectors.toSet());
    }

}
