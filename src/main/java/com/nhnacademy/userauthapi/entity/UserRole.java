package com.nhnacademy.userauthapi.entity;

public enum UserRole {
    ADMIN(0),
    USER(1);

    private final int code;

    UserRole(int code) {
        this.code=code;
    }

    public static UserRole fromCode(int code){
        for(UserRole u: UserRole.values()){
            if(u.code==code){
                return u;
            }
        }
        throw new IllegalArgumentException("Invalid code for UserRole:" +code);
    }
}
