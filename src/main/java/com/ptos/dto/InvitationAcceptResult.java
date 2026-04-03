package com.ptos.dto;

import com.ptos.domain.User;

public record InvitationAcceptResult(User clientUser, User ptUser) {
}
