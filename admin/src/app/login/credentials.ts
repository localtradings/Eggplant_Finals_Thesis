const ADMIN_LOGIN_NAME_PATTERN = /^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$/;
const ADMIN_LOGIN_EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export const MAX_ADMIN_PASSWORD_LENGTH = 128;

export type AdminLoginConfig = {
  loginName: string;
  email: string;
};

export type AdminLoginInput = {
  loginName: string;
  password: string;
};

export function readAdminLoginConfig(
  environment: Record<string, string | undefined>,
): AdminLoginConfig | null {
  const loginName = environment.ADMIN_LOGIN_NAME?.trim() ?? "";
  const email = environment.ADMIN_LOGIN_EMAIL?.trim() ?? "";

  if (!ADMIN_LOGIN_NAME_PATTERN.test(loginName) || !ADMIN_LOGIN_EMAIL_PATTERN.test(email)) {
    return null;
  }

  return { loginName, email };
}

export function parseAdminLoginFormData(formData: FormData): AdminLoginInput | null {
  const loginNameValue = formData.get("name");
  const passwordValue = formData.get("password");

  if (typeof loginNameValue !== "string" || typeof passwordValue !== "string") {
    return null;
  }

  const loginName = loginNameValue.trim();
  if (
    !ADMIN_LOGIN_NAME_PATTERN.test(loginName) ||
    passwordValue.length === 0 ||
    passwordValue.length > MAX_ADMIN_PASSWORD_LENGTH
  ) {
    return null;
  }

  return { loginName, password: passwordValue };
}
