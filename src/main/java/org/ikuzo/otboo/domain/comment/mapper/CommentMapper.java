package org.ikuzo.otboo.domain.comment.mapper;

import org.ikuzo.otboo.domain.comment.dto.CommentDto;
import org.ikuzo.otboo.domain.comment.entity.Comment;
import org.ikuzo.otboo.domain.user.mapper.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = UserMapper.class)
public interface CommentMapper {

    @Mapping(target = "feedId", source = "feed.id")
    @Mapping(target = "author", source = "author", qualifiedByName = "authorDto")
    CommentDto toDto(Comment comment);
}
