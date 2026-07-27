import { createServer } from 'node:http'
import { createSchema, createYoga, createPubSub } from 'graphql-yoga'
import { GraphQLError } from 'graphql'
import { WebSocketServer } from 'ws'
import { useServer } from 'graphql-ws/lib/use/ws'

// 1. PubSub instance for event broadcasting
const pubSub = createPubSub()

// In-memory application store
const jobApplications = [
  {
    id: '1',
    companyName: 'Acme Corp',
    positionTitle: 'Android Engineer',
    status: 'APPLIED',
    appliedDate: '2026-07-20',
    location: 'Remote',
    jobUrl: 'https://example.com/jobs/1',
    notes: 'Great fit for the team',
    version: 1,
  }
]

const schema = createSchema({
  typeDefs: /* GraphQL */ `
    type JobApplication {
      id: ID!
      companyName: String!
      positionTitle: String!
      status: String!
      appliedDate: String!
      location: String
      jobUrl: String
      notes: String
      version: Int!
    }

    type Query {
      jobApplications: [JobApplication!]!
      jobApplication(id: ID!): JobApplication
    }

    input CreateJobApplicationInput {
      companyName: String!
      positionTitle: String!
      status: String!
      appliedDate: String!
      location: String
      jobUrl: String
      notes: String
    }

    type Mutation {
      createJobApplication(input: CreateJobApplicationInput!): JobApplication!
      updateJobApplication(id: ID!, status: String, notes: String, version: Int!): JobApplication!
      deleteJobApplication(id: ID!, version: Int!): Boolean!
    }

    type Subscription {
      jobApplicationUpdated: JobApplication!
    }
  `,
  resolvers: {
    Query: {
      jobApplications: () => jobApplications,
      jobApplication: (_, { id }) => jobApplications.find(app => app.id === id),
    },
    Mutation: {
      createJobApplication: (_, { input }) => {
        const newApp = {
          id: String(jobApplications.length + 1),
          companyName: input.companyName,
          positionTitle: input.positionTitle,
          status: input.status,
          appliedDate: input.appliedDate,
          location: input.location,
          jobUrl: input.jobUrl,
          notes: input.notes,
          version: 1,
        }
        jobApplications.push(newApp)

        // Publish real-time creation
        pubSub.publish('JOB_APPLICATION_UPDATED', { jobApplicationUpdated: newApp })

        return newApp
      },
      updateJobApplication: (_, { id, status, notes, version }) => {
        const app = jobApplications.find(a => a.id === id)
        if (!app) throw new Error(`Application with id ${id} not found`)

        if (version !== app.version) {
          throw new GraphQLError('Conflict: Server data is newer', {
            extensions: {
              code: 'CONFLICT',
              serverVersion: app.version,
            }
          })
        }

        // Nullable mutation arguments act as optional patch fields. The Android
        // client uses an empty string when it intentionally clears notes.
        if (status != null) app.status = status
        if (notes != null) app.notes = notes

        app.version += 1

        pubSub.publish('JOB_APPLICATION_UPDATED', { jobApplicationUpdated: app })

        return app
      },
      deleteJobApplication: (_, { id, version }) => {
        const index = jobApplications.findIndex(a => a.id === id)
        if (index === -1) return false

        const app = jobApplications[index]

        if (version !== app.version) {
          throw new GraphQLError('Conflict: Server data is newer', {
            extensions: {
              code: 'CONFLICT',
              serverVersion: app.version,
            }
          })
        }

        jobApplications.splice(index, 1)
        return true
      },
    },
    Subscription: {
      jobApplicationUpdated: {
        subscribe: () => pubSub.subscribe('JOB_APPLICATION_UPDATED'),
      },
    },
  },
})

// Enable WebSockets protocol for GraphiQL playground
const yoga = createYoga({
  schema,
  graphiql: {
    subscriptionsProtocol: 'WS',
  },
})

const server = createServer(yoga)

// Attach WebSocket Server on /graphql
const wsServer = new WebSocketServer({
  server,
  path: yoga.graphqlEndpoint,
})

// Wire up graphql-ws with the native schema
useServer({ schema }, wsServer)

server.listen(4000, '0.0.0.0', () => {
  console.log('ðŸš€ Server ready at http://0.0.0.0:4000/graphql (HTTP & WS)')
})
