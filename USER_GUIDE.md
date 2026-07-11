# TaskMS User Guide

*A friendly, step-by-step guide. Written so anyone — even a total beginner — can follow along.*

---

## 1. What is this thing?

Imagine an HR office at a university. Every day people do jobs like:

- Posting a job advert
- Reading through people who applied
- Interviewing them
- Sending an offer letter
- Welcoming a new staff member (onboarding)
- Handling leave (holiday) requests
- Doing yearly performance reviews

**TaskMS** is a helper app that keeps a tidy list of all these jobs. Think of it like a
smart to-do board for the whole HR team.

The clever part: TaskMS has a little "brain" (we call it AI). When you create a new job,
the brain **guesses how many days it will take** — based on how long similar jobs took
before. So you know ahead of time if something might be late.

---

## 2. The three kinds of people who use it

Everyone logs in with an email and a password. What you can do depends on **who you are**:

| Who you are | Simple meaning | What they can do |
|-------------|----------------|------------------|
| **HR Officer** | The worker who does the tasks | See and update their own tasks, mark tasks done |
| **HR Manager** | The boss of the officers | Everything an officer can, plus see the big dashboard and team workload |
| **Admin** | The person who runs the whole system | Everything, plus add people, add task types, and retrain the AI brain |

Think of it like a school: Officer = student, Manager = teacher, Admin = principal.

---

## 3. Logging in (your first step, every time)

1. Open your web browser (Chrome, Edge, Firefox — any is fine).
2. In the address bar at the top, type: **http://localhost:8080/** and press **Enter**.
   *(If your team gave you a different web address, use that instead.)*
3. You will see a login box asking for **Email** and **Password**.
4. Type them in and click the **Login** button.

**Test accounts you can try** (great for practice):

| Role | Email | Password |
|------|-------|----------|
| Admin | `admin@uok.ac.rw` | `admin123` |
| HR Manager | `manager@uok.ac.rw` | `manager123` |
| HR Officer | `alice@uok.ac.rw` | `officer123` |

> 🔒 **Keep it safe:** These are practice passwords. In a real office, use your own private
> password and never share it — like your house key.

If your email or password is wrong, the app will politely say so. Just try again.

---

## 4. The Task Board — your main screen

After logging in you land on the **Board**. This is the heart of the app.

The board has columns, like sticky notes on a wall:

- **TO DO** — jobs not started yet
- **IN PROGRESS** — jobs being worked on right now
- **DONE** — finished jobs
- **CANCELLED** — jobs that got dropped

Each task card shows you:

- **Title** — what the job is (e.g. "Interview candidates for Lab Assistant")
- **Who it belongs to** — the officer responsible
- **Complexity** — how hard it is: LOW, MEDIUM, or HIGH
- **Due date** — when it should be finished
- **AI guess** — the number of days the brain thinks it will take
- **⚠️ At risk** — a little warning if the app thinks the job might be late

### The magic "at risk" warning

If a task is coloured or flagged **at risk**, it means: *"Careful! Based on past jobs, this
one might not finish on time."* It's an early heads-up so you can act — like a weather
forecast saying "bring an umbrella."

---

## 5. Doing everyday things

### ➕ Create a new task
1. Click the **New Task** (or **+**) button.
2. Fill in the boxes:
   - **Title** — a short name for the job.
   - **Task type** — pick from the list (e.g. Onboarding, Leave application).
   - **Assignee** — the person who will do it.
   - **Complexity** — LOW / MEDIUM / HIGH.
   - **Due date** — the deadline.
3. Click **Save**.
4. 🎉 The app instantly shows the **AI's guess** for how many days it needs, plus a
   "best case" and "worst case" range. No maths needed from you!

### ✏️ Update a task
Click a task card to open it. Change what you need, then **Save**.

### 🔄 Move a task along
Change its **status** from TO DO → IN PROGRESS when you start, and to DONE when finished.
(You may drag the card, or use a status dropdown — depending on the screen.)

### ✅ Mark a task complete
When a job is really done, mark it **Complete** and tell it the real number of days it took.
This is important — it **teaches the AI brain** to guess better next time. Every completed
task makes the app a little smarter.

---

## 6. The Dashboard (Managers & Admins)

Managers and Admins get an extra screen called the **Dashboard**. It's like the cockpit of
an aeroplane — all the important numbers in one place:

- **Total tasks** and how many are TO DO / IN PROGRESS / DONE.
- **On-time rate** — what fraction of jobs finished before their deadline.
- **Accuracy** — how good the AI's guesses are compared to human guesses.
- **Team workload** — how busy each officer is, and a red flag ⚠️ if someone is
  **overloaded** (too many days of work piled up).

Use it to spot problems early: "Alice has too much on her plate — let me move a task to Bob."

---

## 7. Workload & Suggestions (Managers & Admins)

The app can look at the whole team and gently suggest **who should get the next task** —
usually the person with the lightest load. It's like a fair referee making sure nobody is
buried in work while others are free.

---

## 8. Admin-only powers

If you are an **Admin**, you also get an **Admin** screen where you can:

- **Add / edit / remove people** (users) and set their role.
- **Add / edit / remove task types** (the kinds of jobs).
- **Retrain the AI brain** on demand — a button that tells the model, "Study all the
  finished tasks again and get smarter now."

> With great power comes great responsibility — deleting a user or task type cannot be undone
> easily, so double-check first.

---

## 9. Common questions

**Q: The AI guess looks wrong. Is that bad?**
Not really. When there are only a few finished tasks, the brain is still learning. The more
tasks you complete (with real times), the sharper it gets.

**Q: I logged in but got kicked out later.**
Logins expire after a while (about 8 hours) for safety. Just log in again.

**Q: I can't see the Dashboard.**
That screen is only for Managers and Admins. Ask your Admin if you need access.

**Q: Nothing loads / page is blank.**
The app might not be running. Ask whoever set it up to start it (see the Developer Guide).

---

## 10. Quick cheat-sheet

| I want to... | Do this |
|--------------|---------|
| Log in | Go to http://localhost:8080/ , enter email + password |
| See all tasks | Open the **Board** |
| Add a job | **New Task** → fill form → **Save** |
| Start a job | Move it to **IN PROGRESS** |
| Finish a job | Mark **Complete**, enter real days taken |
| See team stats | Open the **Dashboard** (Manager/Admin) |
| Add a person | **Admin** screen (Admin only) |

That's it — you're ready to use TaskMS. Welcome aboard! 🚀
