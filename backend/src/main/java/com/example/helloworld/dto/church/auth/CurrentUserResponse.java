package com.example.helloworld.dto.church.auth;

import com.example.helloworld.service.church.ChurchMenuService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CurrentUserResponse {
    private boolean authenticated;
    private String uid;
    private String username;
    private String email;
    private String displayName;
    private String photoUrl;
    private List<ChurchMenuService.MenuItemDTO> menus; // 後台菜單
}
