package com.capstone.contractmanagement.utils;

public class MessageKeys {
    public static final String USERNAME_ALREADY_EXISTS = "user.register.user_already_exists";

    public static final String PHONE_NUMBER_ALREADY_EXISTS = "user.login.register.phone-number_already_exists";
    public static final String EMAIL_ALREADY_EXISTS = "user.login.register.email_already_exists";

    public static final String RETRIEVED_ALL_USERS_FAILED = "user.get_user.retrieved_all_users_failed";
    public static final String DELETE_USER_SUCCESSFULLY = "user.delete_user.delete_user_successfully";
    public static final String UPDATE_USER_SUCCESSFULLY = "user.update_user.update_user_successfully";
    public static final String RETRIEVED_ALL_USERS_SUCCESSFULLY = "user.get_user.retrieved_all_users_successfully";
    public static final String RETRIEVED_USER_SUCCESSFULLY = "user.get_user.retrieved_user_successfully";
    public static final String RETRIEVED_USER_FAILED = "user.get_user.retrieved_user_failed";
    public static final String DELETE_USER_FAILED = "user.delete_user.delete_user_failed";
    public static final String LOGIN_SUCCESSFULLY = "user.login.login_successfully";
    public static final String REGISTER_SUCCESSFULLY = "user.login.register_successfully";
    public static final String REGISTER_FAILED = "user.login.register_failed";
    public static final String LOGIN_FAILED = "user.login.login_failed";
    public static final String PASSWORD_NOT_MATCH = "user.register.password_not_match";
    public static final String USER_IS_LOCKED = "user.login.user_is_locked";
    public static final String USER_NOT_FOUND = "user.get_user.user_not_found";
    public static final String OLD_PASSWORD_WRONG = "user.change_password.old_password_wrong";
    public static final String CONFIRM_PASSWORD_NOT_MATCH = "user.confirm_password_not_match";
    public static final String CHANGE_PASSWORD_SUCCESSFULLY = "user.change_password.change_password_successfully";
    public static final String CHANGE_PASSWORD_FAILED = "user.change_password.change_password_failed";
    public static final String OTP_SENT_SUCCESSFULLY = "forgot_password.otp_sent_successfully";
    public static final String OTP_IS_EXPIRED = "forgot_password.otp_is_expired";
    public static final String OTP_NOT_FOUND = "forgot_password.otp_not_found";
    public static final String OTP_VERIFIED_SUCCESSFULLY = "forgot_password.otp_verified_successfully";
    public static final String OTP_INCORRECT = "forgot_password.opt_incorrect";

    public static final String UPDATE_AVATAR_SUCCESSFULLY = "user.update_avatar.update_avatar_successfully";
    public static final String UPLOAD_IMAGES_FILE_MUST_BE_IMAGE = "product.upload_images.file_must_be_image";
    public static final String INSERT_CATEGORY_FAILED = "category.create_category.create_failed";
    public static final String WRONG_PHONE_PASSWORD = "user.login.wrong_phone_password";
    public static final String ROLE_DOES_NOT_EXISTS = "user.login.role_not_exist";
    public static final String USER_DOES_NOT_EXISTS = "user.get_user.user_not_exist";
    public static final String ENABLE_USER_SUCCESSFULLY = "user.enable_user.enable_successfully";
    public static final String BLOCK_USER_SUCCESSFULLY = "user.enable_user.disable_successfully";
    public static final String NOT_ALLOWED = "user.not_allowed";

    public static final String TOKEN_IS_EXPIRED = "user.jwt.token_is_expired";
    public static final String TOKEN_GENERATION_FAILED = "user.login.jwt.token_can_not_create";
    public static final String USER_CANNOT_CHANGE_STATUS = "update_status.user_cannot_change_status";
    public static final String TOKEN_GENERATION_FAILEDSTRING = "user.login.jwt.token_can_not_create";
    public static final String AUTH_TOKEN_MISSING_OR_INVALID = "auth.token_missing_or_invalid";
    public static final String PARTNER_CANNOT_VIEW_OTHER_HOTELS = "hotel.list_hotel.partner_cannot_view_other_hotels";
    public static final String TOKEN_NO_IDENTIFIER = "auth.token_no_identifier";

    public static final String PERMISSION_DENIED = "permission_denied";
    public static final String COURSE_ALREADY_EXISTS = "course.create_course.course_already_exists";
    public static final String COURSE_NOT_FOUND = "course.update_course.course_not_found";
    public static final String COURSE_CREATED_SUCCESSFULLY = "course.create_course.course_created_successfully";
    public static final String COURSE_UPDATED_SUCCESSFULLY = "course.update_course.course_updated_successfully";
    public static final String COURSE_DELETED_SUCCESSFULLY = "course.update_course.course_deleted_successfully";
    public static final String COURSE_FETCHED_SUCCESSFULLY = "course.get_course.course_fetched_successfully";
    public static final String COURSE_IMAGE_UPDATED_SUCCESSFULLY = "course.update_course.course_image_updated_successfully";

    public static final String CHAPTER_CREATED_SUCCESSFULLY = "chapter.create_chapter.chapter_created_successfully";
    public static final String CHAPTER_NOT_FOUND = "chapter.update_chapter.chapter_not_found";
    public static final String CHAPTER_UPDATED_SUCCESSFULLY = "chapter.update_chapter.chapter_updated_successfully";
    public static final String CHAPTER_DELETED_SUCCESSFULLY ="chapter.update_chapter.chapter_deleted_successfully";

    public static final String REVIEW_CREATED_SUCCESSFULLY = "review.create_review.review_created_successfully";
    public static final String REVIEW_NOT_FOUND = "review.update_review.review_not_found";
    public static final String REVIEW_DELETED_SUCCESSFULLY = "review.update_review.review_deleted_successfully";

    public static final String VIDEO_CREATED_SUCCESSFULLY = "video.create_video.video_created_successfully";
    public static final String VIDEO_NOT_FOUND = "video.update_video.video_not_found";
    public static final String VIDEO_UPDATED_SUCCESSFULLY = "video.update_video.video_updated_successfully";
    public static final String VIDEO_DELETED_SUCCESSFULLY = "video.update_video.video_deleted_successfully";

    public static final String LESSON_NOT_FOUND = "lesson.update_lesson.lesson_not_found";
    public static final String INFORMATION_NOT_FOUND = "information.update_information.information_not_found";
    public static final String INFORMATION_CREATED_SUCCESSFULLY = "information.create_information.information_created_successfully";
    public static final String INFORMATION_UPDATED_SUCCESSFULLY = "information.update_information.information_updated_successfully";
    public static final String INFORMATION_DELETED_SUCCESSFULLY = "information.update_information.information_deleted_successfully";
    public static final String RETRIEVED_ALL_PACKAGES_SUCCESSFULLY = "package.list_package.retrieved_all_packages_successfully";

    public static final String REGISTER_PACKAGE_SUCCESSFULLY = "package.register_package.register_successfully";

    public static final String PACKAGE_EXPIRED = "package.package_expired";

    public static final String PACKAGE_EXPIRED_SUCCESSFULLY = "package.package_expired_successfully";

    public static final String RETRIEVED_ALL_PACKAGES_FAILED = "package.list_package.retrieved_all_packages_failed";

    public static final String RETRIEVED_PACKAGE_DETAIL_SUCCESSFULLY = "package.package_detail.retrieved_package_detail_successfully";

    public static final String RETRIEVED_PACKAGE_DETAIL_FAILED = "package.package_detail.retrieved_package_detail_failed";
    public static final String CREATE_PACKAGE_SUCCESSFULLY = "package.create_package.create_successfully";

    public static final String UPDATE_PACKAGE_SUCCESSFULLY = "package.update_package.update_successfully";

    public static final String DELETE_PACKAGE_SUCCESSFULLY = "package.delete_package.delete_successfully";

    public static final String PACKAGE_NOT_FOUND = "package.list_package.package_not_found";

    public static final String USER_DOES_NOT_HAVE_PACKAGE = "package.list_package.user_does_not_have_package";

    public static final String INVALID_PACKAGE_CREATE_REQUEST = "package.package_create.invalid_package_create_request";

    public static final String INVALID_PACKAGE_UPDATE_REQUEST = "package.package_update.invalid_package_update_request";

    public static final String DELETE_PACKAGE_FAILED = "package.delete_package.delete_failed";

    public static final String REGISTER_PACKAGE_FAILED = "package.register_package.register_failed";

    public static final String REVIEW_PERMISSION_DENIED = "review.review_permission_denied";

    public static final String REVIEWS_FETCHED_SUCCESSFULLY = "review.list_review.reviews_fetched_successfully";

    public static final String CREATE_TERM_SUCCESSFULLY = "term.create_term.create_term_successfully";

    public static final String TERM_NOT_FOUND = "term.update_term.term_not_found";

    public static final String TYPE_TERM_NOT_FOUND = "term.update_term.type_term_not_found";

    public static final String UPDATE_TERM_SUCCESSFULLY = "term.update_term.update_term_successfully";

    public static final String GET_ALL_TERMS_SUCCESSFULLY = "term.get_all_terms.get_all_terms_successfully";

    public static final String DELETE_TERM_SUCCESSFULLY = "term.delete_term.delete_term_successfully";

    public static final String GET_TERM_SUCCESSFULLY = "term.get_term.get_term_successfully";

    public static final String GET_ALL_TYPE_TERMS_SUCCESSFULLY = "type_term.get_all_type_terms.get_all_type_terms_successfully";

    public static final String GET_TYPE_TERM_SUCCESSFULLY = "type_term.get_type_term.get_type_term_successfully";

    public static final String CREATE_TASK_SUCCESSFULLY = "task.create_task.create_task_successfully";

    public static final String TASK_NOT_FOUND = "task.update_task.task_not_found";

    public static final String UPDATE_TASK_SUCCESSFULLY = "task.update_task.update_task_successfully";

    public static final String DELETE_TASK_SUCCESSFULLY = "task.delete_task.delete_task_successfully";

    public static final String GET_TASK_SUCCESSFULLY = "task.get_task.get_task_successfully";

    public static final String SEARCH_TASK_SUCCESSFULLY = "task.search_task.search_task_successfully";

    public static final String CREATE_TEMPLATE_SUCCESSFULLY = "template.create_template.create_template_successfull" +
            "y";
    public static final String GET_ALL_TEMPLATES_SUCCESSFULLY = "template.get_all_templates.get_all_templates_successfully";

    public static final String GET_TEMPLATE_SUCCESSFULLY = "template.get_template.get_template_successfully";

    public static final String DELETE_TEMPLATE_SUCCESSFULLY = "template.delete_template.delete_template_successfully";

    public static final String UPDATE_TEMPLATE_SUCCESSFULLY = "template.update_template.update_template_successfully";

    public static final String UPDATE_TEMPLATE_FAILED = "template.update_template.update_template_failed";

    public static final String GET_ALL_CONTRACTS_SUCCESSFULLY = "contract.get_all_contracts.get_all_contracts_successfully";

    public static final String GET_CONTRACT_SUCCESSFULLY = "contract.get_contract.get_contract_successfully";


    public static final String CREATE_CONTRACT_SUCCESSFULLY = "contract.create_contract.create_contract_successfully";

    public static final String UPDATE_CONTRACT_SUCCESSFULLY = "contract.update_contract.update_contract_successfully";

    public static final String DELETE_CONTRACT_SUCCESSFULLY = "contract.delete_contract.delete_contract_successfully";
    public static final String CREATE_TYPE_TERM_SUCCESSFULLY = "term.create_type_term.create_type_term_successfully";
    public static final String PARTY_NOT_FOUND = "party.update_party.party_not_found";
    public static final String DELETE_PARTY_SUCCESSFULLY = "party.delete_party.delete_party_successfully";
    public static final String CREATE_PARTY_SUCCESSFULLY = "party.create_party.create_party_successfully";
    public static final String UPDATE_PARTY_SUCCESSFULLY = "party.update_party.update_party_successfully";
    public static final String GET_PARTY_SUCCESSFULLY = "party.get_party.get_party_successfully";
    public static final String GET_ALL_PARTIES_SUCCESSFULLY = "party.get_all_parties.get_all_parties_successfully";
}
