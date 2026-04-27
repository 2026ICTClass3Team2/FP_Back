package com.example.demo.global.elasticsearch.document;

import com.example.demo.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchDoc {

    @Id
    private String id; // Convert Long userId to String

    // Exact matches or partial matches for names
    @Field(type = FieldType.Text, analyzer = "nori")
    private String nickname;

    @Field(type = FieldType.Text, analyzer = "nori")
    private String username;

    @Field(type = FieldType.Integer)
    private Integer warningCount;

    @Field(type = FieldType.Keyword)
    private String profilePicUrl;

    public UserSearchDoc(User user) {
        this.id = user.getId().toString();
        this.nickname = user.getNickname();
        this.username = user.getUsername();
        this.warningCount = user.getWarningCount();
        this.profilePicUrl = user.getProfilePicUrl();
    }
}



