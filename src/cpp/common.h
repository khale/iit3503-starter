#ifndef __COMMON_H__
#define __COMMON_H__
#include <assert.h>

#define DEBUG_ENABLE 1

#define IIT3503_VERSION_STRING "1.0"

// These surround string literals with the ANSI escape codes to render text with
// color on supporting terminal emulators
#define COLORED(color, str) "\e[" #color "m" str "\e[39m"
#define BLACK(str) COLORED(30, str)
#define RED(str) COLORED(31, str)
#define GREEN(str) COLORED(32, str)
#define YELLOW(str) COLORED(33, str)
#define BLUE(str) COLORED(34, str)
#define MAGENTA(str) COLORED(35, str)
#define CYAN(str) COLORED(36, str)
#define WHITE(str) COLORED(37, str)
#define BRBLACK(str) COLORED(90, str)
#define BRRED(str) COLORED(91, str)
#define BGREEN(str) COLORED(92, str)
#define BRYELLOW(str) COLORED(93, str)
#define BRBLUE(str) COLORED(94, str)
#define BRMAGENTA(str) COLORED(95, str)
#define BRCYAN(str) COLORED(96, str)
#define BRWHTIE(str) COLORED(97, str)

// Surrounds a string literal with the ANSI escape codes to make it render as
// bold in a supporting terminal emulator
#define BOLD(str) "\e[1m" str "\e[22m"

// "Escapes" boldness in a bolded string
#define UNBOLD(str) "\e[22m" str "\e[1m"

// Printout macros for varying occasions. All of these automatically add
// newlines to the end of the format string provided, and `DEBUG_PRINT()`
// includes the filename and line number in the printout. `DEBUG_PRINT()`s are
// only visible if `DEBUG_ENABLE` is defined during compilation.
#define INFO_PRINT(fmt, args...) fprintf(stderr, BOLD(fmt) "\n", ##args)
#define WARNING_PRINT(fmt, args...) fprintf(stderr, BOLD(YELLOW(fmt)) "\n", ##args)
#define DEBUG_PRINT(fmt, args...) fprintf(stderr, BOLD(YELLOW("[" __FILE__ ":%d] " fmt)) "\n", __LINE__, ##args)
#define ERROR_PRINT(fmt, args...) fprintf(stderr, BOLD(RED(fmt)) "\n", ##args)
#define SUGGESTION_PRINT(fmt, args...) fprintf(stderr, BOLD(fmt) "\n", ##args)

#define SUGGESTION_PRINT_NOBRK(fmt, args...) fprintf(stderr, BOLD(fmt), ##args)

// A simple combination of `ERROR_PRINT` and `exit(EXIT_FAILURE)`, intended to
// be used for unrecoverable user errors
#define ERROR_EXIT(fmt, args...)          \
	do {                              \
		ERROR_PRINT(fmt, ##args); \
		exit(EXIT_FAILURE);       \
	} while (0);

#ifndef DEBUG_ENABLE
#	undef DEBUG_PRINT
#	define DEBUG_PRINT(fmt, args...)
#endif

// Branch prediction hints, to be used similarly as in the Linux kernel
#define LIKELY(expr) __builtin_expect(!!(expr), 1)
#define UNLIKELY(expr) __builtin_expect(!!(expr), 0)

#ifndef __clang__
#	define nonnull
#	define nullable
#else
#	define nonnull _Nonnull
#	define nullable _Nullable
#endif

#define PACKED __attribute__((packed))



// A macro that indicates that some place in code should never be reached.
// Without any compile-time configuration, `UNREACHABLE()` will expand to an
// error printout and a call to `abort()`. Defining `UNREACHABLE_RECOVER` during
// compilation disables the `abort()`, while defining `DISABLE_UNREACHABLE`
// disables the check entirely. Defining `UNREACHABLE_ASSUME` implies
// `DISABLE_UNREACHABLE`, and additionally allows the compiler to assume that
// the statement can never be reached during execution.
#define UNREACHABLE()                                  \
	do {                                           \
		UNREACHABLE_ERROR(__FILE__, __LINE__); \
		UNREACHABLE_REACTION();                \
		UNREACHABLE_ASSUMPTION();              \
	} while (0);

#if defined(UNREACHABLE_ASSUME) && !defined(DISABLE_UNREACHABLE)
#	define DISABLE_UNREACHABLE
#endif

#ifndef DISABLE_UNREACHABLE
#	define UNREACHABLE_ERROR(file, line) ERROR_PRINT("[" file ":%d] UNREACHABLE() Reached", line);
#else
#	define UNREACHABLE_ERROR(...)
#endif

#if defined(DISABLE_UNREACHABLE) || defined(UNREACHABLE_RECOVER)
#	define UNREACHABLE_REACTION()
#else
#	define UNREACHABLE_REACTION() abort()
#endif

#ifdef UNREACHABLE_ASSUME
#	define UNREACHABLE_ASSUMPTION() __builtin_unreachable()
#else
#	define UNREACHABLE_ASSUMPTION()
#endif


typedef unsigned short paddr_t;
typedef unsigned short word_t;


#endif
