package com.nhnacademy.userauthapi.entity;

public enum UserStatus {
    ACTIVE(0),
    DORMANT(1),
    DELETED(2);

    private final int code;

    UserStatus(int code){
        this.code=code;
    }

    //숫자 코드를 던져줄테니 해당하는 enum객체를 찾아서 돌려줘!! 라는 변환기 역할
    public static UserStatus fromCode(int code){
        // UserStatus.values() : ACTIVE, DORMANT, DELETED 세가지 배열로 가져옴
        for(UserStatus u: UserStatus.values()){
            if(u.code==code){
                return u;
            }
        }
        throw new IllegalArgumentException("Invalid code for UserStatus:" +code);
    }
}
