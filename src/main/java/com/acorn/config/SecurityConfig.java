package com.acorn.config;

import java.io.IOException;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.acorn.jwt.JwtAuthenticationFilter;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Configuration // 스프링 설정 클래스
@EnableWebSecurity // 스프링 시큐리티 기능을 활성화
@RequiredArgsConstructor
public class SecurityConfig implements WebMvcConfigurer {

	// JwtAuthenticationFilter 의존성 주입
	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
		// CORS 설정과 CSRF 비활성화, 세션 관리 정책 설정
		httpSecurity
				.cors(cors -> cors.configurationSource(corsConfigurationSource())) // CORS 설정
				.csrf(csrf -> csrf.disable()) // CSRF 보호 비활성화
				.httpBasic(httpBasic -> httpBasic.disable()) // HTTP Basic 인증 비활성화
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션 관리 정책 설정
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/", "/auth/**", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**","/api/eateries/**").permitAll() // 특정 요청 허용
						.anyRequest().authenticated() // 그외 다른 요청은 인증 필요
				)
				.exceptionHandling(exception -> exception.authenticationEntryPoint(new FailedAuthenticationEntryPoint())); // 인증 실패 시 처리 로직 설정

		// JWT 인증 필터를 UsernamePasswordAuthenticationFilter 이전에 추가
		httpSecurity.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return httpSecurity.build(); // 설정 완료 후 SecurityFilterChain 반환
	}

	// CORS 설정 메서드
	private CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.addAllowedOrigin("http://localhost:3000"); // React의 로컬 서버 주소
		configuration.setAllowedMethods(List.of("*")); // 모든 HTTP 메서드 허용
		configuration.setAllowedHeaders(List.of("*")); // 모든 헤더 허용
		configuration.setAllowCredentials(true); // 인증 정보 포함 요청 허용 (쿠키 등)

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration); // 모든 경로에 대해 CORS 설정을 적용
		return source;
	}

	// BCryptPasswordEncoder : 스프링 시큐리티에서 제공하는 패스워드 암호화 기능
	@Bean
	public BCryptPasswordEncoder bCryptPasswordEncoder() {
		return new BCryptPasswordEncoder(); // BCryptPasswordEncoder 인스턴스 반환
	}
}

// FailedAuthenticationEntryPoint: 예외 상황(인증 실패)에 발생한 이벤트 처리를 위한 클래스
// AuthenticationEntryPoint : 
// 스프링 시큐리티에서 인증이 필요한 리소스에 접근할 때 사용자가 인증되지 않았을 경우의 진입점을 정의하는 인터페이스, 인증 실패 시 어떤 응답을 반환할지를 결정한다.
class FailedAuthenticationEntryPoint implements AuthenticationEntryPoint {

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException, ServletException {

		response.setContentType("application/json"); // 응답 타입을 JSON으로 설정
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // HTTP 상태 코드를 401로 설정
		response.getWriter().write("{\"code\": \"AF\", \"message\": \"Authorization Failed\"}"); // 예외 상황 발생 시 JSON 응답 작성
	}
}