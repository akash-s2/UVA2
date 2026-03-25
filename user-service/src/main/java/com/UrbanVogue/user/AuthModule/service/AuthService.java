package com.UrbanVogue.user.AuthModule.service;

import com.UrbanVogue.user.AuthModule.dto.*;
import com.UrbanVogue.user.AuthModule.entity.User;
import com.UrbanVogue.user.security.JwtUtil;
import com.UrbanVogue.user.AuthModule.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtUtil jwtUtil;   // for the jwt token generation.

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;     // hashing the passward k liye

    public AuthResponse register(RegisterRequest request) {

        Optional<User> existingUser = userRepository.findByEmail(request.getEmail());

        if(existingUser.isPresent()){
            return new AuthResponse("Email already registered",null);
        }

        User user = new User(
                request.getName(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getPhoneNumber(),
                request.getAddress()
        );
        user.setRole("USER");
        userRepository.save(user);

        return new AuthResponse("User registered successfully",null);
    }

    public AuthResponse login(LoginRequest request) {

        Optional<User> user = userRepository.findByEmail(request.getEmail());

        if(user.isEmpty()){
            return new AuthResponse("User not found",null);
        }

        //if(!user.get().getPassword().equals(request.getPassword()))
        if(!passwordEncoder.matches(request.getPassword(), user.get().getPassword())){
            return new AuthResponse("Invalid password",null);
        }

       // String token = JwtUtil.generateToken(user.get().getEmail());
        String token = jwtUtil.generateToken(user.get().getEmail(),user.get().getRole());
        return new AuthResponse("Login successful", token);
    }
}



